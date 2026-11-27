package com.hotroso.autopay

import com.hotroso.autopay.utils.PaymentNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {

    @Test
    fun parsesVietcombankIncomingCreditAndOrderCode() {
        val res = PaymentNotificationParser.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            message = "SD TK 0123456789 +150,000VND luc 27/11/2026. ND: thanh toan ap849201",
            orderPrefix = "AP",
            onlyKnownBankApps = true
        )

        assertTrue(res.shouldForward)
        assertTrue(res.isCredit)
        assertEquals("AP849201", res.orderCode)
        assertEquals(150000L, res.amount)
        assertEquals("Vietcombank", res.bankName)
    }

    @Test
    fun parsesMbBankDotSeparatorAndCustomPrefix() {
        val res = PaymentNotificationParser.parse(
            packageName = "com.mbmobile",
            title = "MBBank",
            message = "TK 8888 | GD: +1.290.000 VND | ND: SHOP998877",
            orderPrefix = "SHOP",
            onlyKnownBankApps = true
        )

        assertTrue(res.shouldForward)
        assertEquals("SHOP998877", res.orderCode)
        assertEquals(1290000L, res.amount)
        assertEquals("MBBank", res.bankName)
    }

    @Test
    fun ignoresOutgoingDebitNotification() {
        val res = PaymentNotificationParser.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            message = "SD TK 0123456789 -150,000VND luc 27/11/2026. ND: AP849201",
            orderPrefix = "AP",
            onlyKnownBankApps = false
        )

        assertFalse(res.shouldForward)
        assertFalse(res.isCredit)
    }

    @Test
    fun ignoresNonPaymentChatNotificationWithoutOrderCode() {
        val res = PaymentNotificationParser.parse(
            packageName = "com.facebook.orca",
            title = "Friend",
            message = "Hey are we still meeting at 7pm?",
            orderPrefix = "AP",
            onlyKnownBankApps = false
        )

        assertFalse(res.shouldForward)
        assertNull(res.orderCode)
    }

    @Test
    fun blocksUnknownPackageWhenKnownBanksOnlyEnabled() {
        val res = PaymentNotificationParser.parse(
            packageName = "com.random.chat",
            title = "Fake Bank",
            message = "+100,000VND ND: AP123456",
            orderPrefix = "AP",
            onlyKnownBankApps = true
        )

        assertFalse(res.shouldForward)
    }
}
