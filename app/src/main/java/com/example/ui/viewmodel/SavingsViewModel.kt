package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudentEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AdminModel
import com.example.data.repository.SavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole {
    NONE, SISWA, ADMIN
}

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SavingsRepository(application)

    // Current Session State
    private val _userRole = MutableStateFlow(UserRole.NONE)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _currentNis = MutableStateFlow<String?>(null)
    val currentNis: StateFlow<String?> = _currentNis.asStateFlow()

    private val _currentAdmin = MutableStateFlow<AdminModel?>(null)
    val currentAdmin: StateFlow<AdminModel?> = _currentAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    // Flows
    val allStudents: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingWithdrawals: StateFlow<List<TransactionEntity>> = repository.pendingWithdrawals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Student specific observation
    val currentStudent: StateFlow<StudentEntity?> = _currentNis
        .flatMapLatest { nis ->
            if (nis != null) repository.getStudentFlow(nis) else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentStudentTransactions: StateFlow<List<TransactionEntity>> = _currentNis
        .flatMapLatest { nis ->
            if (nis != null) repository.getTransactionsForStudent(nis) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin total calculation (Kas Bersih Sekolah)
    val totalKasSekolah: StateFlow<Long> = allTransactions.map { txList ->
        var masuk = 0L
        var keluar = 0L
        for (tx in txList) {
            val isApproved = tx.status.equals("DISETUJUI", ignoreCase = true) ||
                    tx.status.equals("APPROVED", ignoreCase = true) ||
                    tx.status.equals("BERHASIL", ignoreCase = true)
            if (isApproved) {
                if (tx.tipe.contains("tarik", ignoreCase = true)) {
                    keluar += tx.nominal
                } else {
                    masuk += tx.nominal
                }
            }
        }
        masuk - keluar
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        // Automatically ensure initial data is fresh
        viewModelScope.launch {
            repository.allStudents.firstOrNull()?.firstOrNull()?.let {
                repository.recalculateStudentStats(it.nis)
            }
        }
    }

    fun clearSnackBar() {
        _snackBarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackBarMessage.value = msg
    }

    fun loginStudent(nis: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.studentLogin(nis.trim(), pass.trim())
            _isLoading.value = false
            result.onSuccess {
                _currentNis.value = it.nis
                _userRole.value = UserRole.SISWA
                showMessage("Selamat datang, ${it.nama}!")
                onSuccess()
            }.onFailure {
                showMessage(it.message ?: "Login gagal")
            }
        }
    }

    fun registerStudent(
        nis: String,
        nama: String,
        kelas: String,
        pass: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.studentRegister(nis.trim(), nama.trim(), kelas.trim(), pass.trim())
            _isLoading.value = false
            result.onSuccess {
                _currentNis.value = it.nis
                _userRole.value = UserRole.SISWA
                showMessage("Pendaftaran berhasil! Selamat datang, ${it.nama}")
                onSuccess()
            }.onFailure {
                showMessage(it.message ?: "Pendaftaran gagal")
            }
        }
    }

    fun loginAdmin(username: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.adminLogin(username.trim(), pass.trim())
            _isLoading.value = false
            result.onSuccess {
                _currentAdmin.value = it
                _userRole.value = UserRole.ADMIN
                showMessage("Login Admin berhasil. Selamat bertugas!")
                onSuccess()
            }.onFailure {
                showMessage(it.message ?: "Login Admin gagal")
            }
        }
    }

    fun logout() {
        _userRole.value = UserRole.NONE
        _currentNis.value = null
        _currentAdmin.value = null
        showMessage("Anda telah keluar.")
    }

    fun submitDeposit(
        nominal: Long,
        keterangan: String,
        customNis: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val targetNis = customNis ?: _currentNis.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val desc = keterangan.ifBlank { "Setoran Tabungan" }
            val result = repository.addTransaction(
                nis = targetNis,
                tipe = "Setoran",
                nominal = nominal,
                keterangan = desc
            )
            _isLoading.value = false
            result.onSuccess {
                showMessage("Setoran berhasil! Saldo otomatis bertambah.")
                onComplete(true)
            }.onFailure {
                showMessage(it.message ?: "Gagal memproses setoran")
                onComplete(false)
            }
        }
    }

    fun submitWithdrawal(
        nominal: Long,
        keterangan: String,
        customNis: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val targetNis = customNis ?: _currentNis.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val desc = keterangan.ifBlank { "Penarikan Saldo" }
            val result = repository.addTransaction(
                nis = targetNis,
                tipe = "Penarikan",
                nominal = nominal,
                keterangan = desc
            )
            _isLoading.value = false
            result.onSuccess {
                showMessage("Pengajuan penarikan dikirim! Menunggu ACC Admin.")
                onComplete(true)
            }.onFailure {
                showMessage(it.message ?: "Gagal memproses penarikan")
                onComplete(false)
            }
        }
    }

    fun approveWithdrawal(txId: String, nis: String, adminNote: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.approveWithdrawal(txId, nis, adminNote.ifBlank { "Disetujui Admin" })
            _isLoading.value = false
            result.onSuccess {
                showMessage("Penarikan disetujui (ACC)! Saldo siswa telah dipotong.")
            }.onFailure {
                showMessage(it.message ?: "Gagal menyetujui penarikan")
            }
        }
    }

    fun rejectWithdrawal(txId: String, nis: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.rejectWithdrawal(txId, nis, reason.ifBlank { "Ditolak oleh Admin" })
            _isLoading.value = false
            result.onSuccess {
                showMessage("Penarikan ditolak. Saldo siswa tidak berkurang.")
            }.onFailure {
                showMessage(it.message ?: "Gagal menolak penarikan")
            }
        }
    }

    fun getScriptUrl(): String = repository.getScriptUrl()

    fun saveScriptUrl(url: String) {
        repository.setScriptUrl(url)
        showMessage("Pengaturan URL Web App disimpan.")
    }

    fun pingBackend(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.pingBackend()
            _isLoading.value = false
            result.onSuccess {
                onResult(true, it)
            }.onFailure {
                onResult(false, it.message ?: "Koneksi gagal")
            }
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.resetDemoData()
            _isLoading.value = false
            showMessage("Data contoh berhasil di-reset ke kondisi awal!")
        }
    }
}
