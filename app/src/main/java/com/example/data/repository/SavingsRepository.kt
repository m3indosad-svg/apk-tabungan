package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.MemberEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AdminModel
import com.example.data.model.BaseApiResponse
import com.example.data.model.MemberModel
import com.example.data.model.TransactionModel
import com.example.data.remote.AppsScriptService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavingsRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val memberDao = db.memberDao()
    private val transactionDao = db.transactionDao()
    private val apiService = AppsScriptService.create()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tabungan_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_SCRIPT_URL = "pref_script_url"
        const val DEFAULT_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxISdAKb-RxI3sKN0fM9XJg8avssFjf2ZS9JZQnGoopT5C1HWQQIdKoWDpoXGGdAl3WCg/exec"
        const val DEFAULT_SPREADSHEET_ID = "1EsBQN3wzedlSWQM5kOINma1V8zK1S51jGhTy7-orwac"
    }

    fun getScriptUrl(): String {
        val saved = prefs.getString(PREF_SCRIPT_URL, null)
        return if (!saved.isNullOrBlank()) saved else DEFAULT_SCRIPT_URL
    }

    fun setScriptUrl(url: String) {
        prefs.edit().putString(PREF_SCRIPT_URL, url.trim()).apply()
    }

    // Flows for Compose UI
    val allMembers: Flow<List<MemberEntity>> = memberDao.getAllMembers()
    val allStudents: Flow<List<MemberEntity>> = allMembers
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pendingWithdrawals: Flow<List<TransactionEntity>> = transactionDao.getPendingWithdrawals()

    fun getMemberFlow(noRek: String): Flow<MemberEntity?> = memberDao.getMemberFlow(noRek)
    fun getStudentFlow(nis: String): Flow<MemberEntity?> = getMemberFlow(nis)

    fun getTransactionsForMember(noRek: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForMember(noRek)
    fun getTransactionsForStudent(nis: String): Flow<List<TransactionEntity>> =
        getTransactionsForMember(nis)

    /**
     * Recalculates balances and stats based on the exact Google Apps Script business rules:
     * - Setoran DISETUJUI -> Pemasukan (+)
     * - Penarikan DISETUJUI -> Pengeluaran (-)
     * - Penarikan PENDING -> Pending (Saldo belum dipotong)
     * - DITOLAK -> Diabaikan
     * Saldo = Total Pemasukan - Total Pengeluaran
     */
    suspend fun recalculateMemberStats(noRek: String): MemberEntity? = withContext(Dispatchers.IO) {
        val member = memberDao.getMemberByNoRek(noRek) ?: return@withContext null
        val txs = transactionDao.getTransactionsForMemberSync(noRek)

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
        val updated = member.copy(
            saldo = saldo,
            totalPemasukan = totalPemasukan,
            totalPengeluaran = totalPengeluaran,
            pendingPenarikan = pendingPenarikan
        )
        memberDao.updateMember(updated)
        updated
    }

    suspend fun recalculateStudentStats(nis: String): MemberEntity? = recalculateMemberStats(nis)

    suspend fun resetDemoData() = withContext(Dispatchers.IO) {
        memberDao.clearAll()
        transactionDao.clearAll()
        AppDatabase.seedInitialData(db)
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

    suspend fun memberLogin(noRek: String, pass: String): Result<MemberEntity> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()

        // If cloud URL is available, try remote login first
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "login",
                    noRek = noRek,
                    nis = noRek,
                    password = pass
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val body = response.body()!!
                    val entity = MemberEntity(
                        noRek = body.noRek ?: body.nis ?: noRek,
                        namaLengkap = body.namaLengkap ?: body.nama ?: "Anggota $noRek",
                        alamat = body.alamat ?: body.kelas ?: "",
                        password = pass,
                        saldo = body.saldo ?: 0L,
                        totalPemasukan = body.totalPemasukan ?: 0L,
                        totalPengeluaran = body.totalPengeluaran ?: 0L,
                        pendingPenarikan = body.pendingPenarikan ?: 0L
                    )
                    memberDao.insertMember(entity)
                    // Also fetch transactions
                    syncTransactionsFromRemote(scriptUrl, noRek)
                    return@withContext Result.success(entity)
                } else if (response.body()?.status == "error") {
                    return@withContext Result.failure(Exception(response.body()?.message ?: "Password salah atau No. Rekening tidak terdaftar"))
                }
            } catch (e: Exception) {
                // Network error, fallback to local database
            }
        }

        // Local Room DB check
        val local = memberDao.getMemberByNoRek(noRek)
        if (local != null) {
            if (local.password.isBlank() || local.password == pass) {
                val stats = recalculateMemberStats(noRek) ?: local
                return@withContext Result.success(stats)
            } else {
                return@withContext Result.failure(Exception("Password akun salah!"))
            }
        } else {
            return@withContext Result.failure(Exception("No. Rekening $noRek tidak terdaftar di sistem!"))
        }
    }

    suspend fun studentLogin(nis: String, pass: String): Result<MemberEntity> = memberLogin(nis, pass)

    suspend fun memberRegister(
        noRek: String,
        namaLengkap: String,
        alamat: String,
        pass: String
    ): Result<MemberEntity> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()

        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "register",
                    noRek = noRek,
                    nis = noRek,
                    namaLengkap = namaLengkap,
                    nama = namaLengkap,
                    alamat = alamat,
                    kelas = alamat,
                    password = pass
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val entity = MemberEntity(
                        noRek = noRek,
                        namaLengkap = namaLengkap,
                        alamat = alamat,
                        password = pass,
                        saldo = 0L,
                        totalPemasukan = 0L,
                        totalPengeluaran = 0L,
                        pendingPenarikan = 0L,
                        tanggalDaftar = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    memberDao.insertMember(entity)
                    return@withContext Result.success(entity)
                } else if (response.body()?.status == "error") {
                    return@withContext Result.failure(Exception(response.body()?.message ?: "Registrasi anggota gagal"))
                }
            } catch (e: Exception) {
                // fall through to local
            }
        }

        // Local Room
        val existing = memberDao.getMemberByNoRek(noRek)
        if (existing != null) {
            return@withContext Result.failure(Exception("No. Rekening $noRek sudah terdaftar!"))
        }

        val newMember = MemberEntity(
            noRek = noRek,
            namaLengkap = namaLengkap,
            alamat = alamat,
            password = pass,
            saldo = 0L,
            totalPemasukan = 0L,
            totalPengeluaran = 0L,
            pendingPenarikan = 0L,
            tanggalDaftar = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        memberDao.insertMember(newMember)
        Result.success(newMember)
    }

    suspend fun studentRegister(
        nis: String,
        nama: String,
        kelas: String,
        pass: String
    ): Result<MemberEntity> = memberRegister(nis, nama, kelas, pass)

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
                        nama = "Pengurus Tabungan",
                        role = "Administrator"
                    )
                    syncAdminDashboardFromRemote(scriptUrl)
                    return@withContext Result.success(admin)
                } else if (response.body()?.status == "error") {
                    return@withContext Result.failure(Exception(response.body()?.message ?: "Login Pengurus gagal"))
                }
            } catch (e: Exception) {
                // fallback to local
            }
        }

        // Local Admin credentials verification
        if ((username.equals("admin", ignoreCase = true) && pass == "admin123") ||
            (username.equals("pengurus", ignoreCase = true) && pass == "admin123") ||
            (username.equals("ketua", ignoreCase = true) && pass == "admin123")) {
            val admin = AdminModel(
                username = username,
                nama = if (username.equals("ketua", ignoreCase = true)) "Ketua Pengurus" else "Pengurus Tabungan",
                role = "Super Admin"
            )
            return@withContext Result.success(admin)
        } else {
            return@withContext Result.failure(Exception("Username atau Password Admin salah! (Gunakan admin / admin123)"))
        }
    }

    suspend fun addTransaction(
        noRek: String,
        tipe: String, // "Setoran" or "Penarikan"
        nominal: Long,
        keterangan: String,
        tanggal: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        if (nominal <= 0) {
            return@withContext Result.failure(Exception("Nominal transaksi harus lebih dari Rp 0!"))
        }

        val member = memberDao.getMemberByNoRek(noRek)
            ?: return@withContext Result.failure(Exception("Data anggota No. Rekening $noRek tidak ditemukan!"))

        val isTarik = tipe.equals("Penarikan", ignoreCase = true) || tipe.contains("tarik", ignoreCase = true)
        val normalizedType = if (isTarik) "Penarikan" else "Setoran"

        // Check balance for withdrawal
        if (isTarik && nominal > member.saldo) {
            return@withContext Result.failure(
                Exception("Saldo Anda tidak mencukupi untuk melakukan penarikan Rp %,d! Saldo saat ini: Rp %,d"
                    .format(nominal, member.saldo).replace(',', '.'))
            )
        }

        val initialStatus = if (isTarik) "PENDING" else "DISETUJUI"
        val initialNote = if (isTarik) "Menunggu ACC Pengurus" else "Otomatis Disetujui"
        val txId = "TX-" + SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()) +
                "-" + (100..999).random()

        val scriptUrl = getScriptUrl()
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "addtransaction",
                    noRek = noRek,
                    nis = noRek,
                    tipe = normalizedType,
                    nominal = nominal,
                    keterangan = keterangan,
                    tanggal = tanggal,
                    status = initialStatus
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    val entity = TransactionEntity(
                        id = response.body()?.noRek ?: response.body()?.nis ?: txId,
                        noRek = noRek,
                        namaAnggota = member.namaLengkap,
                        alamatAnggota = member.alamat,
                        tipe = normalizedType,
                        nominal = nominal,
                        keterangan = keterangan,
                        tanggal = tanggal,
                        status = initialStatus,
                        catatanAdmin = initialNote,
                        timestamp = System.currentTimeMillis()
                    )
                    transactionDao.insertTransaction(entity)
                    recalculateMemberStats(noRek)
                    return@withContext Result.success(entity)
                }
            } catch (e: Exception) {
                // fall through to local
            }
        }

        // Local Room persistence
        val entity = TransactionEntity(
            id = txId,
            noRek = noRek,
            namaAnggota = member.namaLengkap,
            alamatAnggota = member.alamat,
            tipe = normalizedType,
            nominal = nominal,
            keterangan = keterangan,
            tanggal = tanggal,
            status = initialStatus,
            catatanAdmin = initialNote,
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(entity)
        recalculateMemberStats(noRek)
        Result.success(entity)
    }

    suspend fun approveWithdrawal(
        txId: String,
        noRek: String,
        adminNote: String = "Disetujui Pengurus"
    ): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "approvewithdrawal",
                    id = txId,
                    noRek = noRek,
                    nis = noRek,
                    adminNote = adminNote
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    updateLocalTxStatus(txId, noRek, "DISETUJUI", adminNote)
                    return@withContext Result.success(response.body()?.message ?: "Penarikan berhasil disetujui (ACC)!")
                }
            } catch (e: Exception) {
                // fallback to local
            }
        }

        updateLocalTxStatus(txId, noRek, "DISETUJUI", adminNote)
        Result.success("Penarikan berhasil disetujui (ACC)! Saldo anggota telah resmi dikurangi.")
    }

    suspend fun rejectWithdrawal(
        txId: String,
        noRek: String,
        reason: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptUrl()
        if (scriptUrl.isNotBlank()) {
            try {
                val response = apiService.executeAction(
                    url = scriptUrl,
                    action = "rejectwithdrawal",
                    id = txId,
                    noRek = noRek,
                    nis = noRek,
                    reason = reason
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    updateLocalTxStatus(txId, noRek, "DITOLAK", reason)
                    return@withContext Result.success(response.body()?.message ?: "Penarikan telah ditolak.")
                }
            } catch (e: Exception) {
                // fallback to local
            }
        }

        updateLocalTxStatus(txId, noRek, "DITOLAK", reason)
        Result.success("Penarikan telah ditolak. Saldo anggota tidak berkurang.")
    }

    private suspend fun updateLocalTxStatus(
        txId: String,
        noRek: String,
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
            recalculateMemberStats(tx.noRek)
        } else {
            recalculateMemberStats(noRek)
        }
    }

    private suspend fun syncTransactionsFromRemote(scriptUrl: String, noRek: String) {
        try {
            val response = apiService.executeAction(
                url = scriptUrl,
                action = "gettransactions",
                noRek = noRek,
                nis = noRek
            )
            if (response.isSuccessful && response.body()?.status == "success") {
                val list = response.body()?.data ?: emptyList()
                val entities = list.map { it.toEntity() }
                transactionDao.insertAll(entities)
                recalculateMemberStats(noRek)
            }
        } catch (ignored: Exception) {}
    }

    suspend fun syncAdminDashboardFromRemote(scriptUrl: String) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.executeAction(url = scriptUrl, action = "admindashboard")
            if (response.isSuccessful && response.body()?.status == "success") {
                val body = response.body()!!

                // Sync members list
                val memberModels = body.members ?: body.students ?: emptyList()
                if (memberModels.isNotEmpty()) {
                    val entities = memberModels.map { m ->
                        val targetRek = m.noRek.ifBlank { m.nis }
                        val targetNama = m.namaLengkap.ifBlank { m.nama }
                        val targetAlamat = m.alamat.ifBlank { m.kelas }
                        val existing = memberDao.getMemberByNoRek(targetRek)
                        MemberEntity(
                            noRek = targetRek,
                            namaLengkap = targetNama,
                            alamat = targetAlamat,
                            password = existing?.password ?: "123456",
                            saldo = m.saldo,
                            totalPemasukan = m.totalPemasukan,
                            totalPengeluaran = m.totalPengeluaran,
                            pendingPenarikan = m.pendingPenarikan,
                            tanggalDaftar = existing?.tanggalDaftar ?: "2026-09-01"
                        )
                    }
                    memberDao.insertAll(entities)
                }

                // Sync all transactions
                val txModels = body.transactions ?: emptyList()
                if (txModels.isNotEmpty()) {
                    val entities = txModels.map { it.toEntity() }
                    transactionDao.insertAll(entities)
                }

                // Recalculate
                val allList = memberDao.getStudentByNis("10012026")
                memberModels.forEach {
                    recalculateMemberStats(it.noRek.ifBlank { it.nis })
                }
            }
        } catch (ignored: Exception) {}
    }

    private fun TransactionModel.toEntity(): TransactionEntity {
        val recNoRek = noRek.ifBlank { nis }
        val recNama = namaAnggota.ifBlank { namaSiswa }
        val recAlamat = alamatAnggota.ifBlank { kelasSiswa }
        return TransactionEntity(
            id = id.ifBlank { "TX-${System.currentTimeMillis()}" },
            noRek = recNoRek,
            namaAnggota = recNama,
            alamatAnggota = recAlamat,
            tipe = tipe,
            nominal = nominal,
            keterangan = keterangan,
            tanggal = tanggal.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            },
            status = status,
            catatanAdmin = catatanAdmin,
            timestamp = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(tanggal)?.time
                    ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )
    }
}
