package com.example

import com.example.data.local.MemberEntity
import com.example.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberSavingsLogicTest {

    @Test
    fun testMemberEntityCreationAndFields() {
        val member = MemberEntity(
            noRek = "10012026",
            namaLengkap = "Budi Hartono",
            alamat = "Jl. Sudirman No. 45, Jakarta",
            password = "password123",
            saldo = 1000000L,
            totalPemasukan = 1000000L,
            totalPengeluaran = 0L,
            pendingPenarikan = 0L
        )

        assertEquals("10012026", member.noRek)
        assertEquals("Budi Hartono", member.namaLengkap)
        assertEquals("Jl. Sudirman No. 45, Jakarta", member.alamat)
        assertEquals("password123", member.password)
        assertEquals(1000000L, member.saldo)
    }

    @Test
    fun testMemberBalanceCalculationRules() {
        val member = MemberEntity(
            noRek = "10012026",
            namaLengkap = "Budi Hartono",
            alamat = "Jl. Sudirman No. 45, Jakarta",
            password = "password123",
            saldo = 0L,
            totalPemasukan = 0L,
            totalPengeluaran = 0L,
            pendingPenarikan = 0L
        )

        val txSetoran = TransactionEntity(
            id = "TX-01",
            noRek = member.noRek,
            namaAnggota = member.namaLengkap,
            alamatAnggota = member.alamat,
            tipe = "Setoran",
            nominal = 500000L,
            keterangan = "Setoran Awal",
            tanggal = "2026-10-01",
            status = "DISETUJUI"
        )

        val txPendingTarik = TransactionEntity(
            id = "TX-02",
            noRek = member.noRek,
            namaAnggota = member.namaLengkap,
            alamatAnggota = member.alamat,
            tipe = "Penarikan",
            nominal = 100000L,
            keterangan = "Tarik Dana Usaha",
            tanggal = "2026-10-01",
            status = "PENDING"
        )

        val txTolak = TransactionEntity(
            id = "TX-03",
            noRek = member.noRek,
            namaAnggota = member.namaLengkap,
            alamatAnggota = member.alamat,
            tipe = "Penarikan",
            nominal = 50000L,
            keterangan = "Tarik Ditolak",
            tanggal = "2026-10-01",
            status = "DITOLAK"
        )

        val txs = listOf(txSetoran, txPendingTarik, txTolak)

        assertEquals("PENDING", txPendingTarik.status)
        assertTrue(txPendingTarik.tipe.equals("Penarikan", ignoreCase = true) || txPendingTarik.tipe.contains("tarik", ignoreCase = true))

        var totalMasuk = 0L
        var totalKeluar = 0L
        var pendingTarik = 0L

        for (tx in txs) {
            val isTarik = tx.tipe.equals("Penarikan", ignoreCase = true) || tx.tipe.contains("tarik", ignoreCase = true)
            val isApproved = tx.status.equals("DISETUJUI", ignoreCase = true)
            val isPending = tx.status.equals("PENDING", ignoreCase = true)

            if (isTarik) {
                if (isApproved) {
                    totalKeluar += tx.nominal
                } else if (isPending) {
                    pendingTarik += tx.nominal
                }
            } else {
                if (isApproved) {
                    totalMasuk += tx.nominal
                }
            }
        }

        val saldo = totalMasuk - totalKeluar
        assertEquals(500000L, saldo)
        assertEquals(100000L, pendingTarik)
        assertEquals(0L, totalKeluar)

        // Now ACC / approve the pending withdrawal
        val txApprovedTarik = txPendingTarik.copy(status = "DISETUJUI")
        val updatedTxs = listOf(txSetoran, txApprovedTarik, txTolak)

        totalMasuk = 0L
        totalKeluar = 0L
        pendingTarik = 0L
        for (tx in updatedTxs) {
            val isTarik = tx.tipe.equals("Penarikan", ignoreCase = true) || tx.tipe.contains("tarik", ignoreCase = true)
            val isApproved = tx.status.equals("DISETUJUI", ignoreCase = true)
            val isPending = tx.status.equals("PENDING", ignoreCase = true)

            if (isTarik) {
                if (isApproved) totalKeluar += tx.nominal
                else if (isPending) pendingTarik += tx.nominal
            } else {
                if (isApproved) totalMasuk += tx.nominal
            }
        }

        val saldoAfterAcc = totalMasuk - totalKeluar
        assertEquals(400000L, saldoAfterAcc)
        assertEquals(100000L, totalKeluar)
        assertEquals(0L, pendingTarik)
    }
}
