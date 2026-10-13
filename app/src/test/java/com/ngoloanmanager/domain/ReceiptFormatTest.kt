package com.ngoloanmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptFormatTest {
    @Test fun formatsTakaWithGroupingAndTwoDecimals() {
        assertEquals("৳ 0.05", ReceiptFormat.taka(5))
        assertEquals("৳ 1,234,567.89", ReceiptFormat.taka(123456789))
        assertEquals("৳ 10,000.00", ReceiptFormat.taka(1_000_000))
    }
    @Test fun fileNameHasNoPersonalDataOrUnsafeCharacters() {
        assertEquals("Receipt_LN-001_20261013.pdf", ReceiptFormat.fileName("LN-001", "2026-10-13"))
        assertEquals("Receipt_.._evil__20261013.pdf", ReceiptFormat.fileName("../evil/", "2026-10-13"))
    }
    @Test fun masksNationalId() {
        assertEquals("", ReceiptFormat.maskId(" "))
        assertEquals("••••6789", ReceiptFormat.maskId("1234566789"))
    }
}
