package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MemberModel
import com.example.data.model.TransactionModel

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val noRek: String,
    val namaLengkap: String,
    val alamat: String,
    val password: String = "123456",
    val saldo: Long = 0L,
    val totalPemasukan: Long = 0L,
    val totalPengeluaran: Long = 0L,
    val pendingPenarikan: Long = 0L,
    val tanggalDaftar: String = ""
) {
    // Backwards compatibility aliases
    val nis: String get() = noRek
    val nama: String get() = namaLengkap
    val kelas: String get() = alamat

    fun toModel(): MemberModel = MemberModel(
        noRek = noRek,
        nis = noRek,
        namaLengkap = namaLengkap,
        nama = namaLengkap,
        alamat = alamat,
        kelas = alamat,
        saldo = saldo,
        totalPemasukan = totalPemasukan,
        totalPengeluaran = totalPengeluaran,
        pendingPenarikan = pendingPenarikan
    )
}

// Type alias for smooth transitions if needed
typealias StudentEntity = MemberEntity

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val noRek: String,
    val namaAnggota: String,
    val alamatAnggota: String,
    val tipe: String, // "Setoran" or "Penarikan"
    val nominal: Long,
    val keterangan: String,
    val tanggal: String,
    val status: String, // "DISETUJUI", "PENDING", "DITOLAK"
    val catatanAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    // Backwards compatibility aliases
    val nis: String get() = noRek
    val namaSiswa: String get() = namaAnggota
    val kelasSiswa: String get() = alamatAnggota

    fun toModel(): TransactionModel = TransactionModel(
        id = id,
        noRek = noRek,
        nis = noRek,
        namaAnggota = namaAnggota,
        namaSiswa = namaAnggota,
        alamatAnggota = alamatAnggota,
        kelasSiswa = alamatAnggota,
        tipe = tipe,
        nominal = nominal,
        keterangan = keterangan,
        tanggal = tanggal,
        status = status,
        catatanAdmin = catatanAdmin
    )
}
