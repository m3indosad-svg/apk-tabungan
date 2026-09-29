package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.StudentModel
import com.example.data.model.TransactionModel

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val nis: String,
    val nama: String,
    val kelas: String,
    val password: String = "123456",
    val saldo: Long = 0L,
    val totalPemasukan: Long = 0L,
    val totalPengeluaran: Long = 0L,
    val pendingPenarikan: Long = 0L,
    val tanggalDaftar: String = ""
) {
    fun toModel(): StudentModel = StudentModel(
        nis = nis,
        nama = nama,
        kelas = kelas,
        saldo = saldo,
        totalPemasukan = totalPemasukan,
        totalPengeluaran = totalPengeluaran,
        pendingPenarikan = pendingPenarikan
    )
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val nis: String,
    val namaSiswa: String,
    val kelasSiswa: String,
    val tipe: String, // "Setoran" or "Penarikan"
    val nominal: Long,
    val keterangan: String,
    val tanggal: String,
    val status: String, // "DISETUJUI", "PENDING", "DITOLAK"
    val catatanAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toModel(): TransactionModel = TransactionModel(
        id = id,
        nis = nis,
        namaSiswa = namaSiswa,
        kelasSiswa = kelasSiswa,
        tipe = tipe,
        nominal = nominal,
        keterangan = keterangan,
        tanggal = tanggal,
        status = status,
        catatanAdmin = catatanAdmin
    )
}
