package com.smartspend.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC")
    suspend fun getAll(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Query("UPDATE transactions SET rawBody = :rawBody, refId = COALESCE(refId, :refId) WHERE fingerprint = :fingerprint AND source = 'SMS'")
    suspend fun updateSmsMetadata(fingerprint: String, rawBody: String?, refId: String?)

    @Query("SELECT fingerprint FROM transactions WHERE source = 'SMS'")
    suspend fun getAllSmsFingerprints(): List<String>

    @Query("SELECT refId FROM transactions WHERE refId IS NOT NULL AND source = 'SMS'")
    suspend fun getAllSmsRefIds(): List<String>

    @Query("UPDATE transactions SET category = :category, customCategory = :customCategory WHERE id = :id")
    suspend fun updateCategory(id: Long, category: Category, customCategory: String? = null)

    @Query("UPDATE transactions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Query("UPDATE transactions SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String?)

    @Query("UPDATE transactions SET splitInfo = :splitInfo WHERE id = :id")
    suspend fun updateSplitInfo(id: Long, splitInfo: String?)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    /** One-time full wipe of all SMS transactions when fingerprint formula changes */
    @Query("DELETE FROM transactions WHERE source = 'SMS'")
    suspend fun deleteAllSmsTransactions(): Int


    @Query("""DELETE FROM transactions WHERE source = 'SMS' AND (
        rawBody LIKE '%statement generated%' OR
        rawBody LIKE '%amt due%' OR
        rawBody LIKE '%amount due%' OR
        rawBody LIKE '%auto-debit date%' OR
        rawBody LIKE '%auto debit date%' OR
        rawBody LIKE '%minimum due%' OR
        rawBody LIKE '%bounce charges%' OR
        rawBody LIKE '%maintain enough balance%' OR
        rawBody LIKE '%payment due date%' OR
        rawBody LIKE '%bill generated%' OR
        rawBody LIKE '%nach mandate%' OR
        rawBody LIKE '%e-mandate%' OR
        rawBody LIKE '%mandate created%' OR
        rawBody LIKE '%mandate registered%' OR
        rawBody LIKE '%autopay registered%' OR
        rawBody LIKE '%standing instruction registered%' OR
        rawBody LIKE '%is due on%' OR
        rawBody LIKE '%due on the%' OR
        rawBody LIKE '%loan a/c for%' OR
        rawBody LIKE '%loan A/c for%' OR
        rawBody LIKE '%for your tata capital loan%' OR
        rawBody LIKE '%for your hdfc loan%' OR
        rawBody LIKE '%for your sbi loan%' OR
        rawBody LIKE '%givacoins%' OR
        rawBody LIKE '%giva coins%' OR
        rawBody LIKE '%supercoins%' OR
        rawBody LIKE '%tataclq coins%' OR
        rawBody LIKE '%myntra credits%' OR
        rawBody LIKE '%myntra cash%' OR
        rawBody LIKE '%loyalty points%' OR
        rawBody LIKE '%reward coins%' OR
        rawBody LIKE '%loan emi%' OR
        rawBody LIKE '%loan amount%' OR
        rawBody LIKE '%as per nach%' OR
        rawBody LIKE '%as per ecs%' OR
        rawBody LIKE '%as per mandate%' OR
        rawBody LIKE '%nach debit%' OR
        rawBody LIKE '%ecs debit%' OR
        rawBody LIKE '%standing instruction executed%' OR
        rawBody LIKE '%auto-debit processed%' OR
        rawBody LIKE '%auto-debit of%' OR
        rawBody LIKE '%auto debit processed%' OR
        rawBody LIKE '%welcome bonus%' OR
        rawBody LIKE '%welcome credit%' OR
        rawBody LIKE '%sign-up bonus%' OR
        rawBody LIKE '%annual fee%' OR
        rawBody LIKE '%joining fee%' OR
        rawBody LIKE '%renewal fee%' OR
        rawBody LIKE '%check your ledger%' OR
        rawBody LIKE '%check ledger for details%' OR
        rawBody LIKE '%please check your ledger%' OR
        rawBody LIKE '%iiflcs%' OR
        rawBody LIKE '%iifl gold%' OR
        rawBody LIKE '%credited to your loan%' OR
        rawBody LIKE '%debited from your loan%' OR
        rawBody LIKE '%credited to your ledger%' OR
        rawBody LIKE '%for your flexi%' OR
        rawBody LIKE '%your od account%' OR
        rawBody LIKE '%your overdraft account%'
    )""")
    suspend fun purgeMandateSms(): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteMany(ids: List<Long>)
}
