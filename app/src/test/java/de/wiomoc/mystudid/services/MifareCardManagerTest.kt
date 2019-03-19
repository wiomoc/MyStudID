package de.wiomoc.mystudid.services

import org.junit.Assert
import org.junit.Test
import de.wiomoc.mystudid.services.MifareCardManager.toIntFromBCD

class MifareCardManagerTest {

    @Test
    fun testBcdFormating() {
        Assert.assertEquals(233445, byteArrayOf(0x12,0x23,0x34,0x45,0x56).toIntFromBCD(1..3))
    }

}