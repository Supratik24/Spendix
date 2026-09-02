package com.smartspend.ai.ui.viewmodel

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartspend.ai.data.*
import com.smartspend.ai.sms.FinancialSmsParser
import com.smartspend.ai.ui.model.ImportStatus
import com.smartspend.ai.ui.model.SpendUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.YearMonth

import android.content.SharedPreferences

class SpendViewModel(private val repository: TransactionRepository, private val prefs: SharedPreferences) : ViewModel() {

    private val _selectedMonth = MutableStateFlow<YearMonth?>(YearMonth.now())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _highlightedCategory = MutableStateFlow<Category?>(null)
    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    private val _isDarkMode = MutableStateFlow<Boolean?>(if (prefs.contains("is_dark_mode")) prefs.getBoolean("is_dark_mode", false) else false)
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)

    private val _filterFlow = combine(
        _selectedMonth,
        _searchQuery,
        _selectedCategory,
        _highlightedCategory
    ) { month, query, cat, highlighted ->
        FilterState(month, query, cat, highlighted)
    }

    private val _currentTab = MutableStateFlow(com.smartspend.ai.ui.model.MainTab.TRANSACTIONS)
    private val _navState = combine(_selectedTransaction, _currentTab) { txn, tab -> Pair(txn, tab) }

    val uiState: StateFlow<SpendUiState> = combine(
        repository.transactions,
        _filterFlow,
        _importStatus,
        _isDarkMode,
        _navState
    ) { txns, filters, status, isDark, nav ->
        val selectedTxn = nav.first
        val tab = nav.second
        val refreshedSelected = if (selectedTxn != null) {
            txns.find { it.id == selectedTxn.id } ?: selectedTxn
        } else null
        SpendUiState(
            transactions = txns,
            selectedMonth = filters.month,
            searchQuery = filters.query,
            selectedCategory = filters.category,
            highlightedCategory = filters.highlighted,
            importStatus = status,
            isDarkMode = isDark,
            selectedTransaction = refreshedSelected,
            currentTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SpendUiState()
    )

    fun toggleTheme() {
        val currentIsDark = _isDarkMode.value ?: false
        val newDark = !currentIsDark
        _isDarkMode.value = newDark
        prefs.edit().putBoolean("is_dark_mode", newDark).apply()
    }

    fun selectPreviousMonth() {
        val current = _selectedMonth.value ?: YearMonth.now()
        _selectedMonth.value = current.minusMonths(1)
        _highlightedCategory.value = null
    }

    fun selectNextMonth() {
        val current = _selectedMonth.value ?: return
        if (current < YearMonth.now()) {
            _selectedMonth.value = current.plusMonths(1)
            _highlightedCategory.value = null
        }
    }

    fun selectCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
        _highlightedCategory.value = null
    }

    fun selectAllTime() {
        _selectedMonth.value = null
        _highlightedCategory.value = null
    }

    fun selectMonth(month: YearMonth?) {
        _selectedMonth.value = month
        _highlightedCategory.value = null
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: Category?) { _selectedCategory.value = category }
    fun setHighlightedCategory(category: Category?) {
        _highlightedCategory.value = if (_highlightedCategory.value == category) null else category
    }


    fun setTab(tab: com.smartspend.ai.ui.model.MainTab) {
        _currentTab.value = tab
    }

    fun selectTransaction(transaction: Transaction?) {
        _selectedTransaction.value = transaction
    }

    // ─── Smart SMS Import ──────────────────────────────────────────────────────
    fun importSms(context: Context) {
        if (_importStatus.value is ImportStatus.Importing) return
        viewModelScope.launch(Dispatchers.IO) {
            _importStatus.value = ImportStatus.Importing
            try {
                val prefs = context.getSharedPreferences("smartexpense_prefs", 0)

                // ── ONE-TIME CLEAN SLATE ─────────────────────────────────────
                // Every time we rewrote the parser the fingerprint formula changed,
                // causing every existing record to be re-inserted as a "new" transaction.
                // Solution: wipe all SMS transactions once per parser version so we start
                // with a clean DB, then let the new stable formula maintain dedup going forward.
                val PARSER_VERSION = "v8_acct_dedup"
                if (prefs.getString("parser_version", "") != PARSER_VERSION) {
                    repository.deleteAllSmsTransactions()   // wipe old data; fresh parse with fixed patterns
                    prefs.edit().putString("parser_version", PARSER_VERSION).apply()
                }

                // ── PURGE NON-TRANSACTION SMS FROM DB ───────────────────────
                // Runs every sync to remove any mandate/promo/NBFC-ledger entries
                // that may have been imported in a previous app version.
                repository.purgeMandateSms()

                // ── LOAD EXISTING KEYS ───────────────────────────────────────
                // After the wipe above these will be empty on first run, which is correct.
                // On subsequent syncs they hold already-saved transactions.
                val existingRefIds       = repository.getAllSmsRefIds().toHashSet()
                val existingFingerprints = repository.getAllSmsFingerprints().toHashSet()

                // In-batch dedup sets — catches duplicates within the same sync run
                // (e.g. PhonePe SMS + Bank SMS for the same UPI payment)
                val batchRefIds       = mutableSetOf<String>()
                val batchFingerprints = mutableSetOf<String>()
                // Extra safety net: amount+type+minute key to catch remaining cross-sender dupes
                val batchAmtTypeMin   = mutableSetOf<String>()

                // Load existing amount+type+minute keys from DB (non-SMS / manual txns
                // don't need dedup, so limit to SMS source only)
                // We'll build it from existing SMS already in the DB
                // (after a fresh wipe this is empty; on re-syncs it prevents re-import)
                repository.getAll().filter { it.source == TransactionSource.SMS }.forEach { txn ->
                    val tsMin = (txn.occurredAt / 60_000L) * 60_000L
                    batchAmtTypeMin.add("${txn.amountPaise}|${txn.type}|$tsMin")
                    
                    // Re-calculate acctDayKey for existing txn from its raw body
                    val acctSuffix = FinancialSmsParser.extractAccountSuffix(txn.rawBody ?: "")
                    if (acctSuffix != null) {
                        val tsDay = (txn.occurredAt / 86_400_000L) * 86_400_000L
                        batchAmtTypeMin.add("${acctSuffix}|${txn.amountPaise}|${txn.type}|$tsDay")
                    }
                }

                var imported = 0
                val projection = arrayOf("address", "body", "date", "date_sent")
                context.contentResolver.query(
                    android.provider.Telephony.Sms.CONTENT_URI,
                    projection, null, null,
                    "date ASC"   // oldest first → earlier SMS wins dedup
                )?.use { cursor ->
                    val addrIdx     = cursor.getColumnIndexOrThrow("address")
                    val bodyIdx     = cursor.getColumnIndexOrThrow("body")
                    val dateIdx     = cursor.getColumnIndexOrThrow("date")
                    val dateSentIdx = cursor.getColumnIndexOrThrow("date_sent")

                    while (cursor.moveToNext()) {
                        val body    = cursor.getString(bodyIdx) ?: continue
                        val address = cursor.getString(addrIdx)
                        val date    = cursor.getLong(dateIdx)
                        val sent    = cursor.getLong(dateSentIdx)
                        val ts      = if (date > 0) date else if (sent > 0) sent else System.currentTimeMillis()

                        val txn = FinancialSmsParser.parse(address, body, ts) ?: continue

                        // ── DEDUP CHECK 1: UPI Ref / UTR (strongest) ─────────
                        if (txn.refId != null &&
                            (txn.refId in existingRefIds || txn.refId in batchRefIds)) continue

                        // ── DEDUP CHECK 2: exact fingerprint match ───────────
                        if (txn.fingerprint in existingFingerprints ||
                            txn.fingerprint in batchFingerprints) {
                            repository.updateSmsMetadata(txn.fingerprint, txn.rawBody, txn.refId)
                            continue
                        }

                        // ── DEDUP CHECK 3: amount + type + minute window ──────
                        // Catches PhonePe/GPay SMS + Bank SMS for same UPI txn when
                        // the UPI ref extraction failed for one of them.
                        val tsMin = (ts / 60_000L) * 60_000L
                        val amtKey = "${txn.amountPaise}|${txn.type}|$tsMin"
                        if (amtKey in batchAmtTypeMin) continue

                        // ── DEDUP CHECK 4: account suffix + amount + type + day ──────
                        // Catches NEFT/IMPS short notifications vs long notifications
                        // e.g. "A/c XX8925 credited Rs. 100" vs "Rs. 100 credited to A/c XX8925 Ref: UTR123"
                        val tsDay = (ts / 86_400_000L) * 86_400_000L
                        val acctSuffix = FinancialSmsParser.extractAccountSuffix(body)
                        val acctDayKeys = if (acctSuffix != null) {
                            listOf(
                                "${acctSuffix}|${txn.amountPaise}|${txn.type}|$tsDay",
                                "${acctSuffix}|${txn.amountPaise}|${txn.type}|${tsDay - 86_400_000L}",
                                "${acctSuffix}|${txn.amountPaise}|${txn.type}|${tsDay + 86_400_000L}"
                            )
                        } else emptyList()
                        if (acctDayKeys.any { it in batchAmtTypeMin }) continue

                        // ── NEW TRANSACTION: insert ──────────────────────────
                        if (repository.save(txn) != -1L) {
                            imported++
                            txn.refId?.let {
                                batchRefIds.add(it)
                                existingRefIds.add(it)
                            }
                            batchFingerprints.add(txn.fingerprint)
                            existingFingerprints.add(txn.fingerprint)
                            batchAmtTypeMin.add(amtKey)
                            if (acctDayKeys.isNotEmpty()) batchAmtTypeMin.add(acctDayKeys[0])
                        }
                    }
                }
                _importStatus.value = ImportStatus.Complete(imported)
            } catch (_: SecurityException) {
                _importStatus.value = ImportStatus.Failed("SMS permission is needed.")
            } catch (_: Exception) {
                _importStatus.value = ImportStatus.Failed("Import failed. Please try again.")
            }
        }
    }

    fun setPermissionDenied() {
        _importStatus.value = ImportStatus.Failed("SMS permission was not granted. You can enable it later in Settings.")
    }

    fun addManual(amount: String, type: TransactionType, category: Category, merchant: String, customCategory: String? = null) {
        val paise = amount.trim().replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong() ?: return
        if (paise <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            repository.save(
                Transaction(
                    amountPaise = paise,
                    type = type,
                    category = category,
                    merchant = merchant.ifBlank { null },
                    occurredAt = now,
                    source = TransactionSource.MANUAL,
                    sender = null,
                    fingerprint = hash("manual|$now|$paise|$merchant|$category")
                )
            )
        }
    }

    fun changeCategory(id: Long, category: Category, customCategory: String? = null) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateCategory(id, category, customCategory)
    }

    fun saveNotes(id: Long, notes: String?) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateNotes(id, notes?.trim()?.ifBlank { null })
    }

    fun saveTags(id: Long, tags: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        val tagsStr = tags.filter { it.isNotBlank() }.joinToString(",").ifBlank { null }
        repository.updateTags(id, tagsStr)
    }

    fun saveSplitInfo(id: Long, splitInfo: String?) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateSplitInfo(id, splitInfo)
    }

    fun delete(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(id)
        if (_selectedTransaction.value?.id == id) {
            _selectedTransaction.value = null
        }
    }

    private fun hash(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private data class FilterState(
        val month: YearMonth?,
        val query: String,
        val category: Category?,
        val highlighted: Category?
    )

    companion object {
        fun factory(repository: TransactionRepository, prefs: SharedPreferences) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SpendViewModel(repository, prefs) as T
        }
    }
}
