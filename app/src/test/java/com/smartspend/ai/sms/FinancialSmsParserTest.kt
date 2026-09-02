package com.smartspend.ai.sms

import com.smartspend.ai.data.Category
import com.smartspend.ai.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialSmsParserTest {

    @Test fun `HDFC debit via UPI with available balance`() {
        val parsed = FinancialSmsParser.parse(
            "VK-HDFCBK",
            "Update! INR 450.00 debited from HDFC Bank A/C **1234 on 25-AUG-26 to VPA swiggy@icici (UPI Ref No 423512345678). Available balance: INR 15,200.00.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(45_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.FOOD, parsed.category)
        assertEquals("Swiggy", parsed.merchant)
    }

    @Test fun `HDFC credit card spend at Amazon`() {
        val parsed = FinancialSmsParser.parse(
            "AD-HDFCBK",
            "HDFC Bank: Rs 1250.00 spent on your Credit Card ending 5678 at AMAZON INDIA on 25-AUG-26. Avl Limit: Rs 45,000.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(125_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.SHOPPING, parsed.category)
        assertEquals("Amazon", parsed.merchant)
    }

    @Test fun `HDFC credit via UPI`() {
        val parsed = FinancialSmsParser.parse(
            "VK-HDFCBK",
            "Money Transfer: Rs 2000.00 credited to HDFC Bank A/c **1234 on 25-AUG-26 by A/c linked to VPA rahul.verma@okhdfcbank.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(200_000L, parsed.amountPaise)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals(Category.INCOME, parsed.category)
        assertEquals("Rahul Verma", parsed.merchant)
    }

    @Test fun `SBI UPI debit to Blinkit`() {
        val parsed = FinancialSmsParser.parse(
            "SBIBNK",
            "Dear SBI UPI User, A/C 1234 debited by Rs. 349.00 on 25Aug26 to VPA blinkit@icici (UPI Ref no 423456789012).",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(34_900L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.GROCERIES, parsed.category)
        assertEquals("Blinkit", parsed.merchant)
    }

    @Test fun `SBI ATM Cash Withdrawal`() {
        val parsed = FinancialSmsParser.parse(
            "SBIBNK",
            "INR 2000.00 withdrawn from ATM using card ending 9999 on 25-Aug-26. Avl Bal Rs 8,000.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(200_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.CASH, parsed.category)
    }

    @Test fun `ICICI Info UPI debit`() {
        val parsed = FinancialSmsParser.parse(
            "VM-ICICIB",
            "Dear Customer, your A/C XX123 is debited with INR 500.00 on 25-Aug-26. Info: UPI/423512345678/Zomato. The Available Balance is INR 23,450.00.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(50_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.FOOD, parsed.category)
        assertEquals("Zomato", parsed.merchant)
    }

    @Test fun `ICICI Salary Credit`() {
        val parsed = FinancialSmsParser.parse(
            "VM-ICICIB",
            "Dear Customer, Account XX123 has been credited with INR 65,000.00 on 25-Aug-26 by Salary. Avl Bal is INR 75,000.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(6_500_000L, parsed.amountPaise)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals(Category.INCOME, parsed.category)
    }

    @Test fun `Axis Bank Uber Cab payment`() {
        val parsed = FinancialSmsParser.parse(
            "AXISBK",
            "Your A/c no. XX1234 is debited for Rs.380.00 on 25-08-2026 towards UBER. Available balance: Rs.14,200.00.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(38_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.TRAVEL, parsed.category)
        assertEquals("Uber", parsed.merchant)
    }

    @Test fun `Refund from merchant credited`() {
        val parsed = FinancialSmsParser.parse(
            "AXISBK",
            "Refund of Rs. 450.00 for order paid earlier has been credited to your card XX9999 from Swiggy.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(45_000L, parsed.amountPaise)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals(Category.REFUND, parsed.category)
        assertEquals("Swiggy", parsed.merchant)
    }

    @Test fun `Kotak Electricity Bill payment`() {
        val parsed = FinancialSmsParser.parse(
            "KOTAKB",
            "Sent Rs.1,450.00 from Kotak Bank AC X1234 for BESCOM electricity bill on 25-Aug-26. UPI Ref 423512345678. Bal: Rs.12000.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(145_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.BILLS, parsed.category)
    }

    @Test fun `Investment via Zerodha`() {
        val parsed = FinancialSmsParser.parse(
            "HDFCBK",
            "Rs. 10,000.00 debited to Zerodha Broking via UPI. Ref 987654.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(1_000_000L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.INVESTMENT, parsed.category)
    }

    @Test fun `Netflix Subscription on Credit Card`() {
        val parsed = FinancialSmsParser.parse(
            "ICICIB",
            "Paid INR 649.00 on Credit Card ending 9999 for Netflix subscription. Avl Limit: Rs 90,000.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(64_900L, parsed.amountPaise)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(Category.ENTERTAINMENT, parsed.category)
        assertEquals("Netflix", parsed.merchant)
    }

    @Test fun `Credit Card bill payment received is Credit`() {
        val parsed = FinancialSmsParser.parse(
            "HDFCBK",
            "Payment of INR 5,000.00 received towards your HDFC Bank Credit Card ending 4321 on 25-AUG-26.",
            1_000L
        )
        requireNotNull(parsed)
        assertEquals(500_000L, parsed.amountPaise)
        assertEquals(TransactionType.CREDIT, parsed.type)
    }

    @Test fun `Ignores pure OTP alerts`() {
        assertNull(FinancialSmsParser.parse("VK-BANK", "Your OTP for Rs. 2,000 payment is 123456. Do not share your password.", 1_000L))
    }

    @Test fun `Fingerprint is stable across timestamp drift`() {
        val t1 = FinancialSmsParser.parse("BANK", "Rs. 100 spent at Swiggy", 1000L)
        val t2 = FinancialSmsParser.parse("BANK", "Rs. 100 spent at Swiggy", 1500L)
        assertEquals(t1?.fingerprint, t2?.fingerprint)
    }
}
