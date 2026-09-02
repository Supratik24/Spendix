package com.smartspend.ai.sms

import com.smartspend.ai.data.*
import java.security.MessageDigest
import java.util.Locale

object FinancialSmsParser {

    private val mandateIgnoreRegex = Regex(
        "\\b(?:" +
            "statement\\s+generated|credit\\s+card\\s+statement|card\\s+statement|" +
            "amt\\s+due\\b|amount\\s+due\\b|amount\\s+payable\\b|" +
            "total\\s+outstanding\\b|outstanding\\s+dues\\b|" +
            "minimum\\s+(?:amount\\s+)?due\\b|min(?:imum)?\\s+due\\b|" +
            "payment\\s+due\\s+(?:date|on\\b|by\\b|reminder)|" +
            "bill\\s+(?:generated|is\\s+ready|payment\\s+due)|" +
            "is\\s+due\\s+on\\s+\\d|" +
            "due\\s+on\\s+(?:the\\s+)?\\d{1,2}|" +
            "for\\s+your\\s+\\w+\\s+(?:capital\\s+)?loan\\b|" +
            "loan\\s+a\\/c\\s+for\\s+(?:month|the\\s+month|emi)|" +
            "your\\s+(?:loan|emi)\\s+payment\\s+(?:of|for)\\s+(?:rs|inr)|" +
            "nach\\s+mandate|e-?mandate\\b|" +
            "mandate\\s+(?:created|registered|success|cancelled|rejected|activated)|" +
            "auto-?debit\\s+(?:set\\s+up|scheduled|created|cancelled|registered|activated|date\\b)|" +
            "autopay\\s+(?:set\\s+up|registered|scheduled|activated|enabled)|" +
            "standing\\s+instruction\\s+(?:registered|set|created)|" +
            "nach\\s+(?:registered|created|activated)|" +
            "si\\s+(?:registered|created|activated)|" +
            "registered\\s+(?:for|with)\\s+(?:nach|autopay|auto-?pay|ecs)|" +
            "emi\\s+(?:due\\b|reminder\\b|bounced\\b|scheduled\\b)|" +
            "your\\s+emi\\s+is\\s+due|your\\s+loan\\s+is\\s+due|" +
            "loan\\s+(?:emi|instalment|installment|amount)\\b|" +
            "emi\\s+of\\s+(?:rs\\.?|inr)|" +
            "emi\\s+for\\s+(?:rs\\.?|inr|the\\s+month)|" +
            "your\\s+emi\\s+(?:of|for)|" +
            "as\\s+per\\s+(?:nach|ecs|mandate|standing\\s+instruction)|" +
            "nach\\s+(?:debit|payment|executed|processed|debited)|" +
            "ecs\\s+(?:debit|payment|executed|processed|debited)|" +
            "standing\\s+instruction\\s+(?:executed|processed|debited)|" +
            "auto.?debit\\s+(?:of|processed|executed|successful)|" +
            "maintain\\s+(?:enough|sufficient)\\s+balance|" +
            "avoid\\s+bounce\\s+charges|bounce\\s+charges|" +
            "pre-?approved\\s+(?:loan|credit\\s+line|offer)|" +
            "loan\\s+(?:offer\\b|pre-?approved|eligib)|" +
            "get\\s+(?:instant\\s+)?(?:loan|credit)\\s+of\\b|" +
            "credit\\s+line\\s+(?:of|up\\s+to)\\b|" +
            "check\\s+your\\s+ledger|please\\s+check\\s+(?:your\\s+)?ledger|" +
            "credited\\s+to\\s+your\\s+(?:loan|ledger|flexi|od)|" +
            "debited\\s+from\\s+your\\s+(?:loan|ledger|flexi|od)|" +
            "for\\s+\\d{5,12}[,.]\\s*(?:rs|inr)|" +
            "your\\s+(?:flexi|od|overdraft)\\s+(?:account|a\\/c)|" +
            "dishonoured\\b|insufficient\\s+(?:funds|balance)|" +
            "low\\s+balance\\s+(?:alert|notification)|" +
            "account\\s+statement\\s+for\\b|" +
            "guaranteed\\s+visa|lifetime\\s+free\\s+visa|" +
            "visa\\s+(?:are\\s+live|now\\s+live|launched\\b)|" +
            "zero\\s+forex\\s+(?:markup|fee|charges)" +
        ")\\b",
        RegexOption.IGNORE_CASE
    )

    private val promotionalIgnoreRegex = Regex(
        "\\b(?:" +
            "lucky\\s+draw|lottery\\b|won\\s+(?:a\\s+)?prize|" +
            "reward\\s+points\\s+(?:earned|added|redeemed)|" +
            "bonus\\s+(?:points\\b|cashback\\b|reward\\b)|" +
            "exclusive\\s+(?:offer|deal)\\s+for\\s+you|" +
            "flash\\s+sale|limited\\s+time\\s+offer|today\\s+only\\b|" +
            "special\\s+offer\\s+for\\s+you|" +
            "click\\s+(?:here|below)\\s+to\\s+(?:claim|avail|redeem|win)|" +
            "refer\\s+and\\s+earn|refer\\s+a\\s+friend|" +
            "upgrade\\s+now\\b|upgrade\\s+your\\s+plan\\b|" +
            "promotional\\s+(?:credit|offer|cashback)|" +
            "free\\s+(?:subscription|membership|trial)\\s+(?:for|of)|" +
            "pre-?approved\\s+for\\s+(?:an?\\s+)?(?:exclusive|special)|" +
            "giva\\s*coins?|givacoins?|" +
            "supercoins?|super\\s+coins?|" +
            "tataclq\\s+(?:coins?|credits?|cash)|tatacl\\s+(?:coins?|credits?)|" +
            "myntra\\s+(?:credits?|cash|wallet\\s+credits?)|" +
            "dominos?\\s+(?:points?|rewards?|loyalty\\s+points?)|" +
            "loyalty\\s+(?:points?|credits?|coins?)\\s+(?:added|earned|credited)|" +
            "reward\\s+coins?\\s+(?:added|earned|credited)|" +
            "earned\\s+\\d+\\s+(?:coins?|points?|credits?)|" +
            "won\\s+\\d+\\s+(?:coins?|points?|credits?)|" +
            "annual\\s+(?:fee|charge|membership)\\s+(?:of|rs|inr)|" +
            "joining\\s+(?:fee|charge)\\s+(?:of|rs|inr)|" +
            "renewal\\s+(?:fee|charge)\\s+(?:of|rs|inr)|" +
            "membership\\s+(?:fee|charge)\\s+(?:of|rs|inr)|" +
            "subscription\\s+(?:fee|charge|renewed)\\s+(?:of|rs|inr)|" +
            "promo\\s+(?:credit|cashback|offer|code)|" +
            "welcome\\s+(?:bonus|credit|cashback|offer)|" +
            "sign-?up\\s+(?:bonus|credit|offer)|" +
            "introductory\\s+(?:offer|credit|cashback)" +
        ")\\b",
        RegexOption.IGNORE_CASE
    )

    private val bnplSenders = setOf(
        "lazypay", "simplbank", "zestmoney", "sliceit",
        "unicards", "kreditbee", "earlysal", "paytmlater", "olamoney", "mobikwik"
    )
    private val promoSenders = setOf(
        "giva", "tataclq", "tatacl", "myntra", "dominos", "ajio", "nykaa", "meesho"
    )

    private val otpRegex = Regex(
        "\\b(?:is your (?:login |secret |verification )?otp|one time password|" +
        "verification code|secret otp|do not share.*password|" +
        "login code|authorization code)\\b",
        RegexOption.IGNORE_CASE
    )

    private val strongCreditRegex = Regex(
        "\\b(?:" +
            "credited\\b|" +
            "credit\\s+(?:of|rs\\.?|inr)\\b|" +
            "cr\\s+(?:rs\\.?|inr)|" +
            "neft\\s+cr(?:edit)?\\b|" +
            "imps\\s+cr(?:edit)?\\b|" +
            "rtgs\\s+cr(?:edit)?\\b|" +
            "received\\s+(?:from|in\\s+your|into|by)|" +
            "money\\s+(?:received|added\\s+to)|" +
            "amount\\s+(?:received|added\\s+to)|" +
            "deposited\\s+(?:in|into)|" +
            "salary\\s+(?:credited|received)|" +
            "payroll\\s+credited|" +
            "refund(?:ed)?\\s*(?:of|to|in|credited|processed|initiated|received)|" +
            "refund\\s+of\\b|refund\\s+received\\b|" +
            "cashback\\s+(?:of|added|credited|received)|" +
            "revers(?:al|ed?)\\s+(?:of|to|credited)|" +
            "credited\\s+back|" +
            "amount\\s+added\\s+to\\b" +
        ")\\b",
        RegexOption.IGNORE_CASE
    )

    private val strongDebitRegex = Regex(
        "\\b(?:" +
            "debited\\b|" +
            "debit\\s+(?:of|rs\\.?|inr)\\b|" +
            "dr\\s+(?:rs\\.?|inr)|" +
            "neft\\s+dr(?:ebit)?\\b|" +
            "imps\\s+dr(?:ebit)?\\b|" +
            "rtgs\\s+dr(?:ebit)?\\b|" +
            "spent\\s+(?:at|on|in|for)|" +
            "paid\\s+(?:to|at|for)|" +
            "purchase(?:d)?\\s+(?:at|of|worth)|" +
            "swiped\\s+at|" +
            "used\\s+(?:at|on\\s+your)|" +
            "charged\\s+(?:on|to\\s+your)|" +
            "payment\\s+(?:of|to|towards)\\s+(?:rs\\.?|inr|\\d)|" +
            "sent\\s+to|" +
            "transferred\\s+to|transfer\\s+to|" +
            "transfer\\s+of\\s+(?:rs\\.?|inr)|" +
            "txn\\s+(?:of|amt)\\s+(?:rs\\.?|inr)|" +
            "withdrawn\\s+(?:from|at)|cash\\s+withdrawal|" +
            "auto-?debited|" +
            "deducted\\s+from\\b" +
        ")\\b",
        RegexOption.IGNORE_CASE
    )

    private val balanceSectionRegex = Regex(
        "\\b(?:available\\s*balance|avail(?:able)?\\.?\\s*bal(?:ance)?|avl\\.?\\s*bal|" +
        "a\\/c\\s*bal|acct\\s*bal|bal(?:ance)?\\s*:|total\\s*bal|clear\\s*bal|" +
        "ledger\\s*bal|credit\\s*limit|avl\\s*limit)[^.]*",
        RegexOption.IGNORE_CASE
    )

    private val amtCurrencyPrefix = Regex(
        "(?:Rs\\.?|INR\\.?)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val amtCurrencyPrefixPaise = Regex(
        "(?:Rs\\.|INR\\.)\\s*\\.\\s*([0-9]{1,2})(?![0-9])",
        RegexOption.IGNORE_CASE
    )
    private val amtVerbPrefix = Regex(
        "(?:debited|credited|paid|spent|sent|withdrawn|refund(?:ed)?|received|" +
        "transfer(?:red)?|deposited|purchase(?:d)?|charged|deducted|used)\\s+" +
        "(?:of|by|with|for|amounting\\s+to)?\\s*(?:Rs\\.?|INR)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val amtTrailingSlash = Regex(
        "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*/\\-"
    )
    private val amtVerbSuffix = Regex(
        "(?<![A-Za-z0-9])([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:debited|credited|paid|spent|withdrawn|transferred)",
        RegexOption.IGNORE_CASE
    )

    private val refLabelPatterns = listOf(
        Regex("\\bUPI\\s*Ref(?:\\s*[Nn]o\\.?)?[:\\-\\s]+([A-Za-z0-9]{8,20})\\b", RegexOption.IGNORE_CASE),
        Regex("Ref\\s*[Nn]o\\.?/(?:[A-Za-z0-9]*/)?([A-Za-z0-9]{8,25})(?:/|\\s|$)", RegexOption.IGNORE_CASE),
        Regex("/[A-Za-z]*UTR/([A-Za-z0-9]{8,25})(?:/|\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\bRef\\s*[Nn]o\\.?[:\\-\\s]+([A-Za-z0-9]{8,20})\\b", RegexOption.IGNORE_CASE),
        Regex("\\bUTR\\s*(?:[Nn]o\\.?)?[:\\-\\s]+([A-Za-z0-9]{8,25})\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:Trans(?:action)?\\s*ID|Txn\\s*ID)[:\\-\\s]+([A-Za-z0-9]{6,20})\\b", RegexOption.IGNORE_CASE),
        Regex("\\bIMPS\\s*Ref\\s*(?:[Nn]o\\.?)?[:\\-\\s]+([A-Za-z0-9]{8,20})\\b", RegexOption.IGNORE_CASE),
        Regex("\\bRRN[:\\-\\s]+([0-9]{8,15})\\b", RegexOption.IGNORE_CASE),
        Regex("\\bOrder\\s*ID[:\\-\\s]+([A-Za-z0-9]{6,20})\\b", RegexOption.IGNORE_CASE)
    )

    private val refEmbeddedUpi = listOf(
        Regex("UPI/(?:P2[PM]/)?([0-9]{10,15})(?:/|\\s|$)", RegexOption.IGNORE_CASE),
        Regex("(?<!/)/([0-9]{10,15})/(?!/)")
    )

    private val creditCardRegex = Regex(
        "\\b(?:credit\\s*card|cc\\s*(?:no|ending|xxxx|\\*)|card\\s+ending\\s+in|credit\\s+card\\s+no)",
        RegexOption.IGNORE_CASE
    )

    fun parse(sender: String?, body: String, receivedAt: Long): Transaction? {
        if (body.isBlank()) return null

        if (mandateIgnoreRegex.containsMatchIn(body)) return null
        if (promotionalIgnoreRegex.containsMatchIn(body)) return null

        val senderLower = (sender ?: "").lowercase()
        if (bnplSenders.any { senderLower.contains(it) }) return null
        if (promoSenders.any { senderLower.contains(it) }) return null

        if (otpRegex.containsMatchIn(body)) return null

        val maskedBody = body
            .replace(Regex("credit\\s*card", RegexOption.IGNORE_CASE), "card_instrument")
            .replace(Regex("debit\\s*card", RegexOption.IGNORE_CASE), "card_instrument")

        val hasCredit = strongCreditRegex.containsMatchIn(maskedBody)
        val hasDebit  = strongDebitRegex.containsMatchIn(maskedBody)
        if (!hasCredit && !hasDebit) return null

        val balanceMatch = balanceSectionRegex.find(body)
        val txnText = if (balanceMatch != null && balanceMatch.range.first > 10)
            body.substring(0, balanceMatch.range.first) else body

        val amountStr = extractAmount(txnText) ?: extractAmount(body) ?: return null
        val paise = amountStr.replace(",", "").trimEnd('/', '-')
            .toBigDecimalOrNull()?.movePointRight(2)?.toLong() ?: return null
        if (paise <= 0L) return null

        val type = resolveType(body, maskedBody, hasCredit, hasDebit) ?: return null

        val normalizedBody   = body.trim().replace("\\s+".toRegex(), " ")
        val normalizedSender = normalizeSender(sender)
        val refId            = extractRefId(normalizedBody)
        val merchant         = extractMerchant(body, type)
        val isCreditCard     = creditCardRegex.containsMatchIn(body)
        val accountSuffix    = extractAccountSuffix(body)

        val tsMin = (receivedAt / 60_000L) * 60_000L
        val tsDay = (receivedAt / 86_400_000L) * 86_400_000L
        val fingerprint = when {
            !refId.isNullOrBlank() ->
                sha256("ref|$normalizedSender|$refId|$paise")
            isCreditCard ->
                sha256("cc|$normalizedSender|$paise|$tsMin")
            !accountSuffix.isNullOrBlank() ->
                sha256("acct|$normalizedSender|$accountSuffix|$paise|$type|$tsDay")
            else ->
                sha256("body|$normalizedSender|$normalizedBody|$tsMin|$paise")
        }

        return Transaction(
            amountPaise  = paise,
            type         = type,
            category     = Categorizer.classify(merchant, body, type),
            merchant     = merchant,
            occurredAt   = receivedAt,
            sender       = sender,
            fingerprint  = fingerprint,
            isCreditCard = isCreditCard,
            refId        = refId,
            rawBody      = body
        )
    }

    private fun extractRefId(body: String): String? {
        for (p in refLabelPatterns) {
            p.find(body)?.groupValues?.getOrNull(1)?.takeIf { it.length >= 6 }?.let { return it }
        }
        for (p in refEmbeddedUpi) {
            p.find(body)?.groupValues?.getOrNull(1)?.takeIf { it.length >= 10 }?.let { return it }
        }
        return null
    }

    fun extractAccountSuffix(body: String): String? =
        Regex("(?:A/c|Account|Acct)\\s+[X*05-9]{0,10}([0-9]{4})\\b", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)

    private fun extractAmount(text: String): String? {
        amtCurrencyPrefixPaise.find(text)?.let { return "0.${it.groupValues[1]}" }
        amtCurrencyPrefix.find(text)?.let { return it.groupValues[1] }
        amtVerbPrefix.find(text)?.let { return it.groupValues[1] }
        amtTrailingSlash.find(text)?.let { return it.groupValues[1] }
        amtVerbSuffix.find(text)?.let { return it.groupValues[1] }
        return null
    }

    private fun resolveType(original: String, masked: String, hasCredit: Boolean, hasDebit: Boolean): TransactionType? {
        if (Regex("payment\\s+(?:of\\s+.*)?received\\s+towards|payment\\s+received\\s+for", RegexOption.IGNORE_CASE).containsMatchIn(original)) return TransactionType.CREDIT
        if (Regex("\\b(?:refund|cashback|reversal|salary\\s+credited)\\b", RegexOption.IGNORE_CASE).containsMatchIn(masked)) return TransactionType.CREDIT
        return when {
            hasCredit && !hasDebit -> TransactionType.CREDIT
            hasDebit && !hasCredit -> TransactionType.DEBIT
            hasCredit && hasDebit  -> {
                val cp = strongCreditRegex.find(masked)?.range?.first ?: Int.MAX_VALUE
                val dp = strongDebitRegex.find(masked)?.range?.first ?: Int.MAX_VALUE
                if (cp < dp) TransactionType.CREDIT else TransactionType.DEBIT
            }
            else -> null
        }
    }

    private fun extractMerchant(body: String, type: TransactionType): String? {
        for (m in Regex("(?:to|from|by|towards|at)?\\s*(?:VPA\\s+)?([A-Za-z0-9._\\-]+@[A-Za-z0-9]+)", RegexOption.IGNORE_CASE).findAll(body)) {
            val handle = m.groupValues[1].substringBefore("@").trim()
            if (handle.isNotEmpty() && !handle.matches(Regex("^\\d+$"))) {
                val c = cleanMerchant(handle.replace(".", " "))
                if (isValidMerchant(c)) return formatTitle(c)
            }
        }
        for (p in listOf(
            Regex("UPI/(?:P2[PM]/)?[0-9]+/([A-Za-z0-9 .&@_\\-]+?)(?:/|\\s+|$)", RegexOption.IGNORE_CASE),
            Regex("Info:\\s*UPI/[0-9]*/([A-Za-z0-9 .&@_\\-]+?)(?:/|\\s+|$)", RegexOption.IGNORE_CASE),
            Regex("Info:\\s*([A-Za-z][A-Za-z0-9 .&_\\-]{2,28})/[0-9]{6,}", RegexOption.IGNORE_CASE)
        )) {
            for (m in p.findAll(body)) {
                val c = cleanMerchant(m.groupValues[1])
                if (isValidMerchant(c)) return formatTitle(c)
            }
        }
        for (p in listOf(
            Regex("(?:spent\\s+at|purchase(?:d)?\\s+at|swiped\\s+at|used\\s+at)\\s+([A-Za-z0-9 .&_\\-]{2,40}?)(?=\\s+(?:on|via|using|ref|upi|avl|bal|dated|for)|[,.]|$)", RegexOption.IGNORE_CASE),
            Regex("(?:paid\\s+to|sent\\s+to|transfer(?:red)?\\s+to)\\s+([A-Za-z0-9 .&_\\-]{2,40}?)(?=\\s+(?:on|via|using|ref|upi|avl|bal|dated)|[,.]|$)", RegexOption.IGNORE_CASE),
            Regex("(?:towards)\\s+([A-Za-z0-9 .&_\\-]{2,40}?)(?=\\s+(?:on|via|using|ref|upi|avl|bal|dated|is)|[,.]|$)", RegexOption.IGNORE_CASE),
            Regex("(?:refund\\s+from|cashback\\s+from|received\\s+from)\\s+([A-Za-z0-9 .&_\\-]{2,40}?)(?=\\s+(?:on|via|using|ref|upi|credited|order)|[,.]|$)", RegexOption.IGNORE_CASE),
            Regex("\\bto\\s+([A-Za-z][A-Za-z0-9 .&_\\-]{1,38}?)(?=\\s+(?:on|via|using|ref|upi|avl|bal|dated)|[,.]|$)", RegexOption.IGNORE_CASE)
        )) {
            for (m in p.findAll(body)) {
                val c = cleanMerchant(m.groupValues.getOrNull(1) ?: continue)
                if (isValidMerchant(c)) return formatTitle(c)
            }
        }
        return null
    }

    private fun cleanMerchant(raw: String): String {
        var s = raw.trim().trimEnd('.', ',', '-', '/')
        s = s.replace(Regex("\\b(?:Rs\\.?|INR)\\b.*", RegexOption.IGNORE_CASE), "").trim()
        s = s.replace(Regex("^(?:VPA|Info|NEFT|IMPS|UPI|P2P|P2M)\\s*", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\s+(?:PVT\\.?\\s*LTD|PRIVATE\\s*LIMITED|LTD|LIMITED|INDIA|SERVICES|PAYMENTS|LLP|INC)\\b", RegexOption.IGNORE_CASE), "")
        return s.trim().trimEnd('.', ',', '-', '/')
    }

    private fun isValidMerchant(c: String): Boolean {
        if (c.length < 2) return false
        val bad = setOf("account","a/c","acct","card","credit card","debit card","your","available","balance","bank","otp","inr","rs","payment","transfer","not you","call","sms","with","to","for","from","on","the","at","is","by","credited","debited","instrument","card_instrument","info")
        if (bad.any { c.equals(it, ignoreCase = true) }) return false
        if (c.matches(Regex("^[0-9]+$"))) return false
        if (c.matches(Regex("^\\d{1,2}[-/]\\w+[-/]\\d{2,4}$"))) return false
        if (c.matches(Regex("^(?:Rs\\.?|INR)?\\s*[0-9,.]+$", RegexOption.IGNORE_CASE))) return false
        return true
    }

    private fun formatTitle(name: String) = name.split(" ").filter { it.isNotBlank() }.joinToString(" ") { w ->
        if (w.all { it.isUpperCase() } && w.length <= 3) w
        else w.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun normalizeSender(sender: String?): String? {
        if (sender == null) return null
        val parts = sender.split("-", limit = 2)
        return if (parts.size > 1 && parts[0].length <= 3) parts[1].uppercase() else sender.uppercase()
    }

    private fun sha256(input: String) = MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}

object Categorizer {
    fun classify(merchant: String?, body: String, type: TransactionType): Category {
        val text = "${merchant.orEmpty()} $body".lowercase(Locale.getDefault())
        if (type == TransactionType.CREDIT) {
            return when {
                listOf("salary","payroll","stipend","dividend","interest","wages").any(text::contains) -> Category.INCOME
                listOf("refund","cashback","reversal","returned","credited back","reimbursement").any(text::contains) -> Category.REFUND
                else -> Category.INCOME
            }
        }
        return when {
            listOf("zepto","blinkit","instamart","bigbasket","bb daily","dunzo","dmart","grofers","spencer","grocery","supermarket","jiomart","country delight","milk","kirana","provision").any(text::contains) -> Category.GROCERIES
            listOf("swiggy","zomato","restaurant","cafe","starbucks","mcdonald","burger","pizza","kfc","domino","bakery","coffee","eatsure","biryani","haldiram","taco bell","faasos","subway","dining","dhaba","pastry","sweets").any(text::contains) -> Category.FOOD
            listOf("uber","ola","rapido","metro","irctc","fuel","petrol","diesel","hpcl","bpcl","iocl","shell","toll","fastag","flight","indigo","air india","vistara","spicejet","makemytrip","yatra","goibibo","redbus","cab","taxi","parking","train","airline","travel").any(text::contains) -> Category.TRAVEL
            listOf("amazon","flipkart","myntra","mall","meesho","ajio","nykaa","tata cliq","zara","h&m","uniqlo","croma","reliance digital","vijay sales","decathlon","lenskart","clothing","fashion","shopping","lifestyle","westside","pantaloons").any(text::contains) -> Category.SHOPPING
            listOf("electricity","recharge","broadband","bill","jio","airtel","vi ","vodafone","bescom","tneb","cesc","mseb","water","gas","cylinder","piped gas","wifi","act fibernet","hathway","tata play","dish tv","dth","rent","maintenance","society","utility","postpaid","prepaid").any(text::contains) -> Category.BILLS
            listOf("pharmacy","hospital","medical","apollo","pharmeasy","1mg","practo","netmeds","medplus","doctor","clinic","lab","diagnostics","gym","cult","fitness","healthcare","dental","medicine","chemist","opticals").any(text::contains) -> Category.HEALTH
            listOf("netflix","spotify","movie","bookmyshow","pvr","inox","cinepolis","prime video","hotstar","disney","youtube","gaming","steam","playstation","sonyliv","zee5","apple.com","google play","cinema","theatre","audible","gaana","wynk").any(text::contains) -> Category.ENTERTAINMENT
            listOf("zerodha","groww","upstox","angel one","indmoney","kuvera","mutual fund","sip","nps","uti","amc","stock","securities","gold","coindcx","wazirx","equity","bse","nse","cdsl","nsdl").any(text::contains) -> Category.INVESTMENT
            listOf("atm","cash withdrawal","nfs","cash wdl","atm wdl").any(text::contains) -> Category.CASH
            listOf("upi","transfer","sent to","paid to","vpa","imps","neft","rtgs","cred","phonepe","google pay","gpay","paytm","bhim").any(text::contains) -> Category.TRANSFER
            else -> Category.OTHER
        }
    }
}