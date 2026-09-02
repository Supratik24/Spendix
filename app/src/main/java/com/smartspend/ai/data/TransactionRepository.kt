package com.smartspend.ai.data

class TransactionRepository(private val dao: TransactionDao) {
    val transactions = dao.observeAll()

    suspend fun save(transaction: Transaction) = dao.insert(transaction)
    suspend fun deleteAllSmsTransactions() = dao.deleteAllSmsTransactions()
    suspend fun purgeMandateSms() = dao.purgeMandateSms()
    suspend fun getAllSmsFingerprints() = dao.getAllSmsFingerprints()
    suspend fun getAllSmsRefIds() = dao.getAllSmsRefIds()
    suspend fun getAll() = dao.getAll()
    suspend fun updateSmsMetadata(fingerprint: String, rawBody: String?, refId: String?) =
        dao.updateSmsMetadata(fingerprint, rawBody, refId)
    suspend fun updateCategory(id: Long, category: Category, customCategory: String? = null) = dao.updateCategory(id, category, customCategory)
    suspend fun updateNotes(id: Long, notes: String?) = dao.updateNotes(id, notes)
    suspend fun updateTags(id: Long, tags: String?) = dao.updateTags(id, tags)
    suspend fun updateSplitInfo(id: Long, splitInfo: String?) = dao.updateSplitInfo(id, splitInfo)
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun delete(id: Long) = dao.delete(id)
}
