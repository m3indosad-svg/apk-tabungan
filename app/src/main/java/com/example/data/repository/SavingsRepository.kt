package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.StudentEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AdminModel
import com.example.data.model.BaseApiResponse
import com.example.data.model.StudentModel
import com.example.data.model.TransactionModel
import com.example.data.remote.AppsScriptService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SavingsRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val studentDao = db.studentDao()
    private val transactionDao = db.transactionDao()
    private val apiService = AppsScriptService.create()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tabungan_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_SCRIPT_URL = "pref_script_url"
        const val DEFAULT_SPREADSHEET_ID = "1EsBQN3wzedlSWQM5kOINma1V8zK1S51jGhTy7-orwac"
    }

    fun getScriptUrl(): String {
        return prefs.getString(PREF_SCRIPT_URL, "") ?: ""
    }

    fun setScriptUrl(url: String) {
        prefs.edit().putString(PREF_SCRIPT_URL, url.trim()).apply()
    }

    // Flows for Compose UI
    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pendingWithdrawals: Flow<List<TransactionEntity>> = transactionDao.getPendingWithdrawals()

    fun getStudentFlow(nis: String): Flow<StudentEntity?> = studentDao.getStudentFlow(nis)
    fun getTransactionsForStudent(nis: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForStudent(nis)

    /**
     * Recalculates balances and stats based on the exact Google Apps Script business rules:
     * - Setoran DISETUJUI -> Pemasukan (+)
     * - Penarikan DISETUJUI -> Pengeluaran (-)
     * - Penarikan PENDING -> Pending (Saldo belum dipotong)
     * - DITOLAK -> Diabaikan
     * Saldo = Total Pemasukan - Total Pengeluaran
     */
    suspend fun recalculateStudentStats(nis: String): StudentEntity? = withContext(Dispatchers.IO) {
        val student = studentDao.getStudentByNis(nis) ?: return@withContext null
        val txs = transactionDao.getTransactionsForStudentSync(nis)

        var totalPemasukan = 0L
        var totalPengeluaran = 0L
        var pendingPenarikan = 0L

        for (tx in txs) {
            val isTarik = tx.tipe.equals("Penarikan", ignoreCase = true) ||
                    tx.tipe.contains("tarik", ignoreCase = true)
            val isApproved = tx.status.equals("DISETUJUI", ignoreCase = true) ||
                    tx.status.equals("APPROVED", ignoreCase = true) ||
                    tx.status.equals("BERHASIL", ignoreCase = true)
            val isPending = tx.status.equals("PENDING", ignoreCase = true) ||
                    tx.status.equals("MENUNGGU", ignoreCase = true)

            if (isTarik) {
                if (isApproved) {
                    totalPengeluaran += tx.nominal
                } else if (isPending) {
                    pendingPenarikan += tx.nominal
                }
            } else {
                if (isApproved) {
                    totalPemasukan += tx.nominal
                }
            }
        }

        val saldo = totalPemasukan - totalPengeluaran
        val updated = student.copy(
            saldo = saldo,
            totalPemasukan = totalPemasukan,
            totalPengeluaran = totalPengeluaran,
            pendingPenarikan = pendingPenarikan
        )
        studentDao.updateStudent(updated)
        updated
    }

    suspend fun pingBackend(): Result<String> = withContext(Dispatchers.IO) {
        val url = getScriptUrl()
        if (url.isBlank()) {
            return@withContext Result.failure(Exception("URL Web App belum diatur di Pengaturan"))
        }

        try {
            val response = apiService.executeAction(url = url, action = "ping")
            if (response.isSuccessful && response.body()?.status == "success") {
                val body = response.body()!!
                val title = body.spreadsheetTitle ?: "Google Spreadsheet"
                Result.success("Terhubung ke $title (ID: ${body.spreadsheetId ?: DEFAULT_SPREADSHEET_ID})")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Gagal terhubung ke Google Apps Script"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun studentLogin(nis: String, pass: String): Result<StudentEntity> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()

        // If cloud URL is available, try remote login first
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "login",
                    nis = nis,
                    password = pass
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val body = response.body()!!
                    val entity = StudentEntity(
                        nis = body.nis ?: nis,
                        nama = body.nama ?: "Siswa $nis",
                        kelas = body.kelas ?: "",
                        password = pass,
                        saldo = body.saldo ?: 0L,
                        totalPemasukan = body.totalPemasukan ?: 0L,
                        totalPengeluaran = body.totalPengeluaran ?: 0L,
                        pendingPenarikan = body.pendingPenarikan ?: 0L
                    )
                    studentDao.insertStudent(entity)
                    // Also fetch transactions
                    syncTransactionsFromRemote(scriptUrl, nis)
                    return@withContext Result.success(entity)
                } else if (response.body()?.status == "error") {
                    return@withContext Result.failure(Exception(response.body()?.message ?: "Password salah atau NIS tidak terdaftar"))
                }
            } catch (e: Exception) {
                // Network error, fallback to local database
            }
        }

        // Local Room DB check
        val local = studentDao.getStudentByNis(nis)
        if (local != null) {
            if (local.password.isBlank() || local.password == pass) {
                val stats = recalculateStudentStats(nis) ?: local
                return@withContext Result.success(stats)
            } else {
                return@withContext Result.failure(Exception("Password salah!"))
            }
        } else {
            return@withContext Result.failure(Exception("NIS $nis tidak terdaftar di sistem!"))
        }
    }

    suspend fun studentRegister(
        nis: String,
        nama: String,
        kelas: String,
        pass: String
    ): Result<StudentEntity> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()

        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "register",
                    nis = nis,
                    nama = nama,
                    kelas = kelas,
                    password = pass
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val entity = StudentEntity(
                        nis = nis,
                        nama = nama,
                        kelas = kelas,
                        password = pass,
                        saldo = 0L,
                        totalPemasukan = 0L,
                        totalPengeluaran = 0L,
                        pendingPenarikan = 0L,
                        tanggalDaftar = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    studentDao.insertStudent(entity)
                    return@withContext Result.success(entity)
                } else if (response.body()?.status == "error") {
                    return@withContext Result.failure(Exception(response.body()?.message ?: "Registrasi gagal"))
                }
            } catch (e: Exception) {
                // fall through to local
            }
        }

        // Local Room
        val existing = studentDao.getStudentByNis(nis)
        if (existing != null) {
            return@withContext Result.failure(Exception("NIS $nis sudah terdaftar!"))
        }

        val newStudent = StudentEntity(
            nis = nis,
            nama = nama,
            kelas = kelas,
            password = pass,
            saldo = 0L,
            totalPemasukan = 0L,
            totalPengeluaran = 0L,
            pendingPenarikan = 0L,
            tanggalDaftar = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        studentDao.insertStudent(newStudent)
        Result.success(newStudent)
    }

    suspend fun adminLogin(username: String, pass: String): Result<AdminModel> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()

        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "adminlogin",
                    username = username,
                    password = pass
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val admin = response.body()?.admin ?: AdminModel(
                        username = username,
                        nama = "Admin Tabungan",
                        role = "Administrator"
                    )
                    syncAdminDashboardFromRemote(scriptUrl)
                    return@withContext Result.success(admin)
                } else if (response.body()?.status == "error") {
                    return@withContext Result.failure(Exception(response.body()?.message ?: "Login Admin gagal"))
                }
            } catch (e: Exception) {
                // fallback to local
            }
        }

        // Local Admin credentials verification
        if ((username.equals("admin", ignoreCase = true) && pass == "admin123") ||
            (username.equals("kepala", ignoreCase = true) && pass == "admin123")) {
            val admin = AdminModel(
                username = username,
                nama = if (username.equals("kepala", ignoreCase = true)) "Kepala Administrasi" else "Administrator Tabungan",
                role = "Super Admin"
            )
            return@withContext Result.success(admin)
        } else {
            return@withContext Result.failure(Exception("Username atau Password Admin salah! (Gunakan admin / admin123)"))
        }
    }

    suspend fun addTransaction(
        nis: String,
        tipe: String, // "Setoran" or "Penarikan"
        nominal: Long,
        keterangan: String,
        tanggal: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        if (nominal <= 0) {
            return@withContext Result.failure(Exception("Nominal transaksi harus lebih dari Rp 0!"))
        }

        val student = studentDao.getStudentByNis(nis)
            ?: return@withContext Result.failure(Exception("Data siswa NIS $nis tidak ditemukan!"))

        val isTarik = tipe.equals("Penarikan", ignoreCase = true) || tipe.contains("tarik", ignoreCase = true)
        val normalizedType = if (isTarik) "Penarikan" else "Setoran"

        // Check balance for withdrawal
        if (isTarik && nominal > student.saldo) {
            return@withContext Result.failure(
                Exception("Saldo Anda tidak mencukupi untuk melakukan penarikan Rp %,d! Saldo saat ini: Rp %,d"
                    .format(nominal, student.saldo).replace(',', '.'))
            )
        }

        val initialStatus = if (isTarik) "PENDING" else "DISETUJUI"
        val initialNote = if (isTarik) "Menunggu ACC Admin" else "Otomatis Disetujui"
        val txId = "TX-" + SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()) +
                "-" + (100..999).random()

        val scriptUrl = getScriptUrl()
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "addtransaction",
                    nis = nis,
                    tipe = normalizedType,
                    nominal = nominal,
                    keterangan = keterangan,
                    tanggal = tanggal,
                    status = initialStatus
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val entity = TransactionEntity(
                        id = response.body()?.nis ?: txId,
                        nis = nis,
                        namaSiswa = student.nama,
                        kelasSiswa = student.kelas,
                        tipe = normalizedType,
                        nominal = nominal,
                        keterangan = keterangan,
                        tanggal = tanggal,
                        status = initialStatus,
                        catatanAdmin = initialNote,
                        timestamp = System.currentTimeMillis()
                    )
                    transactionDao.insertTransaction(entity)
                    recalculateStudentStats(nis)
                    return@withContext Result.success(entity)
                }
            } catch (e: Exception) {
                // fall through to local
            }
        }

        // Local Room persistence
        val entity = TransactionEntity(
            id = txId,
            nis = nis,
            namaSiswa = student.nama,
            kelasSiswa = student.kelas,
            tipe = normalizedType,
            nominal = nominal,
            keterangan = keterangan,
            tanggal = tanggal,
            status = initialStatus,
            catatanAdmin = initialNote,
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(entity)
        recalculateStudentStats(nis)
        Result.success(entity)
    }

    suspend fun approveWithdrawal(
        txId: String,
        nis: String,
        adminNote: String = "Disetujui Admin"
    ): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "approvewithdrawal",
                    id = txId,
                    nis = nis,
                    adminNote = adminNote
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    updateLocalTxStatus(txId, nis, "DISETUJUI", adminNote)
                    return@withContext Result.success(response.body()?.message ?: "Penarikan berhasil disetujui (ACC)!")
                }
            } catch (e: Exception) {
                // fallback to local
            }
        }

        updateLocalTxStatus(txId, nis, "DISETUJUI", adminNote)
        Result.success("Penarikan berhasil disetujui (ACC)! Saldo siswa telah resmi dikurangi.")
    }

    suspend fun rejectWithdrawal(
        txId: String,
        nis: String,
        reason: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "rejectwithdrawal",
                    id = txId,
                    nis = nis,
                    reason = reason
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    updateLocalTxStatus(txId, nis, "DITOLAK", reason)
                    return@withContext Result.success(response.body()?.message ?: "Penarikan telah ditolak.")
                }
            } catch (e: Exception) {
                // fallback to local
            }
        }

        updateLocalTxStatus(txId, nis, "DITOLAK", reason)
        Result.success("Penarikan telah ditolak. Saldo siswa tidak berkurang.")
    }

    private suspend fun updateLocalTxStatus(
        txId: String,
        nis: String,
        status: String,
        note: String
    ) {
        val tx = transactionDao.getTransactionById(txId)
        if (tx != null) {
            val updated = tx.copy(
                status = status,
                catatanAdmin = note
            )
            transactionDao.updateTransaction(updated)
            recalculateStudentStats(tx.nis)
        } else {
            recalculateStudentStats(nis)
        }
    }

    private suspend fun syncTransactionsFromRemote(scriptUrl: String, nis: String) {
        try {
            val response = apiService.executeAction(
                url = scriptUrl,
                action = "gettransactions",
                nis = nis
            )
            if (response.isSuccessful && response.body()?.status == "success") {
                val list = response.body()?.data ?: emptyList()
                val entities = list.map { it.toEntity() }
                transactionDao.insertAll(entities)
                recalculateStudentStats(nis)
            }
        } catch (ignored: Exception) {}
    }

    private suspend fun syncAdminDashboardFromRemote(scriptUrl: String) {
        try {
            val response = apiService.executeAction(
                url = scriptUrl,
                action = "getadmindashboard"
            )
            if (response.isSuccessful && response.body()?.status == "success") {
                val students = response.body()?.students ?: emptyList()
                val txs = response.body()?.transactions ?: emptyList()

                val studentEntities = students.map {
                    StudentEntity(
                        nis = it.nis,
                        nama = it.nama,
                        kelas = it.kelas,
                        saldo = it.saldo,
                        totalPemasukan = it.totalPemasukan,
                        totalPengeluaran = it.totalPengeluaran,
                        pendingPenarikan = it.pendingPenarikan
                    )
                }
                studentDao.insertAll(studentEntities)

                val txEntities = txs.map { it.toEntity() }
                transactionDao.insertAll(txEntities)
            }
        } catch (ignored: Exception) {}
    }

    suspend fun resetDemoData() = withContext(Dispatchers.IO) {
        studentDao.clearAll()
        transactionDao.clearAll()
        AppDatabase.seedInitialData(db)
    }

    private fun TransactionModel.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id.ifBlank { "TX-${System.currentTimeMillis()}-${(100..999).random()}" },
            nis = nis,
            namaSiswa = namaSiswa,
            kelasSiswa = kelasSiswa,
            tipe = if (tipe.contains("tarik", ignoreCase = true)) "Penarikan" else "Setoran",
            nominal = nominal,
            keterangan = keterangan,
            tanggal = tanggal,
            status = status.uppercase(Locale.getDefault()),
            catatanAdmin = catatanAdmin,
            timestamp = System.currentTimeMillis()
        )
    }
}
