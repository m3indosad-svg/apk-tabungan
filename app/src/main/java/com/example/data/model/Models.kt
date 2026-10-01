package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MemberModel(
    @Json(name = "noRek") val noRek: String = "",
    @Json(name = "nis") val nis: String = "", // fallback jika Google Sheets lama memakai nis
    @Json(name = "namaLengkap") val namaLengkap: String = "",
    @Json(name = "nama") val nama: String = "", // fallback jika Google Sheets memakai nama
    @Json(name = "alamat") val alamat: String = "",
    @Json(name = "kelas") val kelas: String = "", // fallback jika Google Sheets memakai kelas
    @Json(name = "saldo") val saldo: Long = 0L,
    @Json(name = "totalPemasukan") val totalPemasukan: Long = 0L,
    @Json(name = "totalPengeluaran") val totalPengeluaran: Long = 0L,
    @Json(name = "pendingPenarikan") val pendingPenarikan: Long = 0L
) {
    val nomorRekening: String get() = noRek.ifBlank { nis }
    val displayNama: String get() = namaLengkap.ifBlank { nama }
    val displayAlamat: String get() = alamat.ifBlank { kelas }
}

@JsonClass(generateAdapter = true)
data class TransactionModel(
    @Json(name = "id") val id: String = "",
    @Json(name = "noRek") val noRek: String = "",
    @Json(name = "nis") val nis: String = "",
    @Json(name = "namaAnggota") val namaAnggota: String = "",
    @Json(name = "namaSiswa") val namaSiswa: String = "",
    @Json(name = "alamatAnggota") val alamatAnggota: String = "",
    @Json(name = "kelasSiswa") val kelasSiswa: String = "",
    @Json(name = "tipe") val tipe: String = "Setoran", // "Setoran" or "Penarikan"
    @Json(name = "nominal") val nominal: Long = 0L,
    @Json(name = "keterangan") val keterangan: String = "",
    @Json(name = "tanggal") val tanggal: String = "",
    @Json(name = "status") val status: String = "DISETUJUI", // "DISETUJUI", "PENDING", "DITOLAK"
    @Json(name = "catatanAdmin") val catatanAdmin: String = ""
) {
    val nomorRekening: String get() = noRek.ifBlank { nis }
    val displayNama: String get() = namaAnggota.ifBlank { namaSiswa }
    val displayAlamat: String get() = alamatAnggota.ifBlank { kelasSiswa }
}

@JsonClass(generateAdapter = true)
data class AdminModel(
    @Json(name = "username") val username: String = "",
    @Json(name = "nama") val nama: String = "",
    @Json(name = "role") val role: String = ""
)

@JsonClass(generateAdapter = true)
data class BaseApiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String? = null,
    @Json(name = "noRek") val noRek: String? = null,
    @Json(name = "nis") val nis: String? = null,
    @Json(name = "namaLengkap") val namaLengkap: String? = null,
    @Json(name = "nama") val nama: String? = null,
    @Json(name = "alamat") val alamat: String? = null,
    @Json(name = "kelas") val kelas: String? = null,
    @Json(name = "saldo") val saldo: Long? = null,
    @Json(name = "totalPemasukan") val totalPemasukan: Long? = null,
    @Json(name = "totalPengeluaran") val totalPengeluaran: Long? = null,
    @Json(name = "pendingPenarikan") val pendingPenarikan: Long? = null,
    @Json(name = "admin") val admin: AdminModel? = null,
    @Json(name = "data") val data: List<TransactionModel>? = null,
    @Json(name = "members") val members: List<MemberModel>? = null,
    @Json(name = "students") val students: List<MemberModel>? = null,
    @Json(name = "transactions") val transactions: List<TransactionModel>? = null,
    @Json(name = "pendingWithdrawals") val pendingWithdrawals: List<TransactionModel>? = null,
    @Json(name = "spreadsheetId") val spreadsheetId: String? = null,
    @Json(name = "spreadsheetTitle") val spreadsheetTitle: String? = null
)
