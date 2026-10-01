package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY namaLengkap ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE noRek = :noRek LIMIT 1")
    fun getMemberFlow(noRek: String): Flow<MemberEntity?>

    @Query("SELECT * FROM members WHERE noRek = :noRek LIMIT 1")
    suspend fun getMemberByNoRek(noRek: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("DELETE FROM members")
    suspend fun clearAll()

    // Compatibility methods
    @Query("SELECT * FROM members ORDER BY namaLengkap ASC")
    fun getAllStudents(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE noRek = :nis LIMIT 1")
    fun getStudentFlow(nis: String): Flow<MemberEntity?>

    @Query("SELECT * FROM members WHERE noRek = :nis LIMIT 1")
    suspend fun getStudentByNis(nis: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: MemberEntity)
}

typealias StudentDao = MemberDao

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE noRek = :noRek ORDER BY timestamp DESC")
    fun getTransactionsForMember(noRek: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE noRek = :noRek")
    suspend fun getTransactionsForMemberSync(noRek: String): List<TransactionEntity>

    // Compatibility methods
    @Query("SELECT * FROM transactions WHERE noRek = :nis ORDER BY timestamp DESC")
    fun getTransactionsForStudent(nis: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE noRek = :nis")
    suspend fun getTransactionsForStudentSync(nis: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE tipe = 'Penarikan' AND (status = 'PENDING' OR status = 'MENUNGGU') ORDER BY timestamp DESC")
    fun getPendingWithdrawals(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
