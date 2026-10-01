package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MemberEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    fun studentDao(): StudentDao = memberDao()
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tabungan_anggota.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialData(getDatabase(context))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val memberDao = database.memberDao()
            val transactionDao = database.transactionDao()

            // Pre-seed sample Anggota data
            val defaultMember1 = MemberEntity(
                noRek = "10012026",
                namaLengkap = "Budi Hartono",
                alamat = "Jl. Sudirman No. 45, Jakarta Selatan",
                password = "123456",
                saldo = 1500000L,
                totalPemasukan = 1500000L,
                totalPengeluaran = 0L,
                pendingPenarikan = 100000L,
                tanggalDaftar = "2026-09-01"
            )

            val defaultMember2 = MemberEntity(
                noRek = "10012027",
                namaLengkap = "Siti Nurhaliza",
                alamat = "Komplek Melati Indah Blok B3, Bandung",
                password = "123456",
                saldo = 2250000L,
                totalPemasukan = 2250000L,
                totalPengeluaran = 0L,
                pendingPenarikan = 0L,
                tanggalDaftar = "2026-09-02"
            )

            val defaultMember3 = MemberEntity(
                noRek = "10012028",
                namaLengkap = "Ahmad Supriyadi",
                alamat = "Dusun Krajan RT 02 / RW 03, Surabaya",
                password = "123456",
                saldo = 750000L,
                totalPemasukan = 900000L,
                totalPengeluaran = 150000L,
                pendingPenarikan = 0L,
                tanggalDaftar = "2026-09-03"
            )

            val defaultMemberLegacy = MemberEntity(
                noRek = "2026101",
                namaLengkap = "Ahmad Dani",
                alamat = "Jl. Diponegoro No. 88, Semarang",
                password = "123456",
                saldo = 500000L,
                totalPemasukan = 500000L,
                totalPengeluaran = 0L,
                pendingPenarikan = 50000L,
                tanggalDaftar = "2026-09-01"
            )

            memberDao.insertAll(listOf(defaultMember1, defaultMember2, defaultMember3, defaultMemberLegacy))

            // Initial transactions
            val initialTx1 = TransactionEntity(
                id = "TX-ANG-001",
                noRek = "10012026",
                namaAnggota = "Budi Hartono",
                alamatAnggota = "Jl. Sudirman No. 45, Jakarta Selatan",
                tipe = "Setoran",
                nominal = 1500000L,
                keterangan = "Simpanan Pokok & Wajib Awal",
                tanggal = "2026-09-01",
                status = "DISETUJUI",
                catatanAdmin = "Otomatis Sistem",
                timestamp = System.currentTimeMillis() - 86400000L * 5
            )

            val initialTx2 = TransactionEntity(
                id = "TX-PENDING-002",
                noRek = "10012026",
                namaAnggota = "Budi Hartono",
                alamatAnggota = "Jl. Sudirman No. 45, Jakarta Selatan",
                tipe = "Penarikan",
                nominal = 100000L,
                keterangan = "Penarikan Tabungan Sukarela",
                tanggal = "2026-09-28",
                status = "PENDING",
                catatanAdmin = "Menunggu ACC Pengurus",
                timestamp = System.currentTimeMillis() - 3600000L * 2
            )

            val initialTx3 = TransactionEntity(
                id = "TX-ANG-003",
                noRek = "10012027",
                namaAnggota = "Siti Nurhaliza",
                alamatAnggota = "Komplek Melati Indah Blok B3, Bandung",
                tipe = "Setoran",
                nominal = 2250000L,
                keterangan = "Setoran Tabungan Bulanan",
                tanggal = "2026-09-10",
                status = "DISETUJUI",
                catatanAdmin = "Otomatis Sistem",
                timestamp = System.currentTimeMillis() - 86400000L * 3
            )

            val initialTx4 = TransactionEntity(
                id = "TX-ANG-004",
                noRek = "10012028",
                namaAnggota = "Ahmad Supriyadi",
                alamatAnggota = "Dusun Krajan RT 02 / RW 03, Surabaya",
                tipe = "Setoran",
                nominal = 900000L,
                keterangan = "Setoran Simpanan Anggota",
                tanggal = "2026-09-15",
                status = "DISETUJUI",
                catatanAdmin = "Otomatis Sistem",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            )

            val initialTx5 = TransactionEntity(
                id = "TX-ANG-005",
                noRek = "10012028",
                namaAnggota = "Ahmad Supriyadi",
                alamatAnggota = "Dusun Krajan RT 02 / RW 03, Surabaya",
                tipe = "Penarikan",
                nominal = 150000L,
                keterangan = "Penarikan Kebutuhan Darurat",
                tanggal = "2026-09-20",
                status = "DISETUJUI",
                catatanAdmin = "Disetujui Pengurus",
                timestamp = System.currentTimeMillis() - 86400000L
            )

            transactionDao.insertAll(listOf(initialTx1, initialTx2, initialTx3, initialTx4, initialTx5))
        }
    }
}
