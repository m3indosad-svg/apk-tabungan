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
    entities = [StudentEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tabungan_siswa.db"
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
            val studentDao = database.studentDao()
            val transactionDao = database.transactionDao()

            // Pre-seed matching the Google Apps Script initial data
            val defaultStudent1 = StudentEntity(
                nis = "2026101",
                nama = "Ahmad Dani",
                kelas = "12 IPA 1",
                password = "123456",
                saldo = 500000L,
                totalPemasukan = 500000L,
                totalPengeluaran = 0L,
                pendingPenarikan = 50000L,
                tanggalDaftar = "2026-09-01"
            )

            val defaultStudent2 = StudentEntity(
                nis = "2026102",
                nama = "Siti Rahmawati",
                kelas = "12 IPA 2",
                password = "123456",
                saldo = 750000L,
                totalPemasukan = 750000L,
                totalPengeluaran = 0L,
                pendingPenarikan = 0L,
                tanggalDaftar = "2026-09-02"
            )

            val defaultStudent3 = StudentEntity(
                nis = "2026103",
                nama = "Budi Santoso",
                kelas = "11 IPS 1",
                password = "123456",
                saldo = 250000L,
                totalPemasukan = 300000L,
                totalPengeluaran = 50000L,
                pendingPenarikan = 0L,
                tanggalDaftar = "2026-09-03"
            )

            studentDao.insertAll(listOf(defaultStudent1, defaultStudent2, defaultStudent3))

            // Initial transactions
            val initialTx1 = TransactionEntity(
                id = "TX-INIT-001",
                nis = "2026101",
                namaSiswa = "Ahmad Dani",
                kelasSiswa = "12 IPA 1",
                tipe = "Setoran",
                nominal = 500000L,
                keterangan = "Saldo Awal Pembukaan",
                tanggal = "2026-09-01",
                status = "DISETUJUI",
                catatanAdmin = "Otomatis Sistem",
                timestamp = System.currentTimeMillis() - 86400000L * 5
            )

            val initialTx2 = TransactionEntity(
                id = "TX-PENDING-002",
                nis = "2026101",
                namaSiswa = "Ahmad Dani",
                kelasSiswa = "12 IPA 1",
                tipe = "Penarikan",
                nominal = 50000L,
                keterangan = "Beli Buku Paket Kimia",
                tanggal = "2026-09-28",
                status = "PENDING",
                catatanAdmin = "Menunggu ACC Admin",
                timestamp = System.currentTimeMillis() - 3600000L * 2
            )

            val initialTx3 = TransactionEntity(
                id = "TX-INIT-003",
                nis = "2026102",
                namaSiswa = "Siti Rahmawati",
                kelasSiswa = "12 IPA 2",
                tipe = "Setoran",
                nominal = 750000L,
                keterangan = "Setoran Tabungan Mingguan",
                tanggal = "2026-09-10",
                status = "DISETUJUI",
                catatanAdmin = "Otomatis Sistem",
                timestamp = System.currentTimeMillis() - 86400000L * 3
            )

            val initialTx4 = TransactionEntity(
                id = "TX-INIT-004",
                nis = "2026103",
                namaSiswa = "Budi Santoso",
                kelasSiswa = "11 IPS 1",
                tipe = "Setoran",
                nominal = 300000L,
                keterangan = "Tabungan Awal Siswa",
                tanggal = "2026-09-15",
                status = "DISETUJUI",
                catatanAdmin = "Otomatis Sistem",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            )

            val initialTx5 = TransactionEntity(
                id = "TX-INIT-005",
                nis = "2026103",
                namaSiswa = "Budi Santoso",
                kelasSiswa = "11 IPS 1",
                tipe = "Penarikan",
                nominal = 50000L,
                keterangan = "Iuran Lomba OSIS",
                tanggal = "2026-09-20",
                status = "DISETUJUI",
                catatanAdmin = "Disetujui Admin",
                timestamp = System.currentTimeMillis() - 86400000L
            )

            transactionDao.insertAll(listOf(initialTx1, initialTx2, initialTx3, initialTx4, initialTx5))
        }
    }
}
