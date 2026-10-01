package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MemberEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AdminModel
import com.example.data.repository.SavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole {
    NONE, ANGGOTA, ADMIN;

    companion object {
        val SISWA = ANGGOTA // Compatibility
    }
}

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SavingsRepository(application)

    // Current Session State
    private val _userRole = MutableStateFlow(UserRole.NONE)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _currentNoRek = MutableStateFlow<String?>(null)
    val currentNoRek: StateFlow<String?> = _currentNoRek.asStateFlow()
    val currentNis: StateFlow<String?> get() = currentNoRek

    private val _currentAdmin = MutableStateFlow<AdminModel?>(null)
    val currentAdmin: StateFlow<AdminModel?> = _currentAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    // Flows
    val allMembers: StateFlow<List<MemberEntity>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStudents: StateFlow<List<MemberEntity>> get() = allMembers

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingWithdrawals: StateFlow<List<TransactionEntity>> = repository.pendingWithdrawals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Member specific observation
    val currentMember: StateFlow<MemberEntity?> = _currentNoRek
        .flatMapLatest { noRek ->
            if (noRek != null) repository.getMemberFlow(noRek) else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val currentStudent: StateFlow<MemberEntity?> get() = currentMember

    val currentMemberTransactions: StateFlow<List<TransactionEntity>> = _currentNoRek
        .flatMapLatest { noRek ->
            if (noRek != null) repository.getTransactionsForMember(noRek) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentStudentTransactions: StateFlow<List<TransactionEntity>> get() = currentMemberTransactions

    // Total Kas Bersih Tabungan Anggota
    val totalKasTabungan: StateFlow<Long> = allTransactions.map { txList ->
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
    val totalKasSekolah: StateFlow<Long> get() = totalKasTabungan

    init {
        // Fresh initial calculations
        viewModelScope.launch {
            repository.allMembers.firstOrNull()?.firstOrNull()?.let {
                repository.recalculateMemberStats(it.noRek)
            }
        }
    }

    fun clearSnackBar() {
        _snackBarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackBarMessage.value = msg
    }

    fun loginMember(noRek: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.memberLogin(noRek.trim(), pass.trim())
            _isLoading.value = false
            result.onSuccess {
                _currentNoRek.value = it.noRek
                _userRole.value = UserRole.ANGGOTA
                _snackBarMessage.value = "Selamat datang, ${it.namaLengkap}!"
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Login gagal"
            }
        }
    }

    fun loginStudent(nis: String, pass: String, onSuccess: () -> Unit = {}) = loginMember(nis, pass, onSuccess)

    fun registerMember(
        noRek: String,
        namaLengkap: String,
        alamat: String,
        pass: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.memberRegister(
                noRek = noRek.trim(),
                namaLengkap = namaLengkap.trim(),
                alamat = alamat.trim(),
                pass = pass.trim()
            )
            _isLoading.value = false
            result.onSuccess {
                _currentNoRek.value = it.noRek
                _userRole.value = UserRole.ANGGOTA
                _snackBarMessage.value = "Akun Anggota ${it.namaLengkap} berhasil didaftarkan!"
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Registrasi gagal"
            }
        }
    }

    fun registerStudent(nis: String, nama: String, kelas: String, pass: String, onSuccess: () -> Unit = {}) =
        registerMember(nis, nama, kelas, pass, onSuccess)

    fun loginAdmin(username: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.adminLogin(username.trim(), pass.trim())
            _isLoading.value = false
            result.onSuccess {
                _currentAdmin.value = it
                _userRole.value = UserRole.ADMIN
                _snackBarMessage.value = "Berhasil masuk sebagai ${it.nama}"
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Login admin gagal"
            }
        }
    }

    fun depositMember(nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) {
        val noRek = _currentNoRek.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val ket = if (keterangan.isBlank()) "Setoran Tabungan Anggota" else keterangan
            val result = repository.addTransaction(
                noRek = noRek,
                tipe = "Setoran",
                nominal = nominal,
                keterangan = ket
            )
            _isLoading.value = false
            result.onSuccess {
                _snackBarMessage.value = "Setoran Rp %,d berhasil disimpan dan disetujui!".format(nominal).replace(',', '.')
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Setoran gagal"
            }
        }
    }

    fun depositStudent(nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) = depositMember(nominal, keterangan, onSuccess)

    fun withdrawMember(nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) {
        val noRek = _currentNoRek.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val ket = if (keterangan.isBlank()) "Penarikan Tabungan Anggota" else keterangan
            val result = repository.addTransaction(
                noRek = noRek,
                tipe = "Penarikan",
                nominal = nominal,
                keterangan = ket
            )
            _isLoading.value = false
            result.onSuccess {
                _snackBarMessage.value = "Permohonan penarikan Rp %,d terkirim. Menunggu persetujuan Pengurus (ACC)."
                    .format(nominal).replace(',', '.')
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Penarikan gagal"
            }
        }
    }

    fun withdrawStudent(nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) = withdrawMember(nominal, keterangan, onSuccess)

    fun adminDepositForMember(noRek: String, nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val ket = if (keterangan.isBlank()) "Setoran Langsung via Pengurus" else keterangan
            val result = repository.addTransaction(
                noRek = noRek,
                tipe = "Setoran",
                nominal = nominal,
                keterangan = ket
            )
            _isLoading.value = false
            result.onSuccess {
                _snackBarMessage.value = "Setoran Rp %,d untuk No. Rek $noRek berhasil disetujui!".format(nominal).replace(',', '.')
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Setoran gagal"
            }
        }
    }

    fun adminDepositForStudent(nis: String, nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) =
        adminDepositForMember(nis, nominal, keterangan, onSuccess)

    fun adminWithdrawForMember(noRek: String, nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val ket = if (keterangan.isBlank()) "Penarikan Langsung via Pengurus" else keterangan
            val result = repository.addTransaction(
                noRek = noRek,
                tipe = "Penarikan",
                nominal = nominal,
                keterangan = ket
            )
            _isLoading.value = false
            result.onSuccess {
                // Auto approve because performed by Admin
                repository.approveWithdrawal(it.id, noRek, "Penarikan Langsung oleh Pengurus")
                _snackBarMessage.value = "Penarikan Rp %,d untuk No. Rek $noRek berhasil diproses!".format(nominal).replace(',', '.')
                onSuccess()
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Penarikan gagal"
            }
        }
    }

    fun adminWithdrawForStudent(nis: String, nominal: Long, keterangan: String, onSuccess: () -> Unit = {}) =
        adminWithdrawForMember(nis, nominal, keterangan, onSuccess)

    fun approveWithdrawal(txId: String, noRek: String, adminNote: String = "Disetujui Pengurus") {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.approveWithdrawal(txId, noRek, adminNote)
            _isLoading.value = false
            result.onSuccess {
                _snackBarMessage.value = it
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Gagal menyetujui transaksi"
            }
        }
    }

    fun rejectWithdrawal(txId: String, noRek: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.rejectWithdrawal(txId, noRek, reason)
            _isLoading.value = false
            result.onSuccess {
                _snackBarMessage.value = it
            }.onFailure {
                _snackBarMessage.value = it.message ?: "Gagal menolak transaksi"
            }
        }
    }

    fun submitDeposit(amount: Long, desc: String, noRek: String? = null) {
        if (noRek != null) {
            adminDepositForMember(noRek, amount, desc)
        } else {
            depositMember(amount, desc)
        }
    }

    fun submitWithdrawal(amount: Long, desc: String, noRek: String? = null) {
        if (noRek != null) {
            adminWithdrawForMember(noRek, amount, desc)
        } else {
            withdrawMember(amount, desc)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.resetDemoData()
            _isLoading.value = false
            _snackBarMessage.value = "Data demo tabungan berhasil di-reset ke awal."
        }
    }

    fun pingBackend(onResult: (Boolean, String) -> Unit) {
        testPing { msg, success ->
            onResult(success, msg)
        }
    }

    fun testPing(onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.pingBackend()
            _isLoading.value = false
            result.onSuccess {
                onResult(it, true)
            }.onFailure {
                onResult(it.message ?: "Gagal ping backend", false)
            }
        }
    }

    fun getScriptUrl(): String = repository.getScriptUrl()

    fun saveScriptUrl(url: String) {
        repository.setScriptUrl(url)
        _snackBarMessage.value = "URL Web App Google Apps Script berhasil disimpan"
    }

    fun logout() {
        _userRole.value = UserRole.NONE
        _currentNoRek.value = null
        _currentAdmin.value = null
        _snackBarMessage.value = "Berhasil keluar"
    }
}
