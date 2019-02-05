package de.wiomoc.mystudid.services

import android.app.PendingIntent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.MifareClassic
import android.support.annotation.StringRes
import android.util.Log
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.activities.MainActivity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.nfcManager
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.util.*

object CardManager {

    fun enableForegroundDispatch(activity: MainActivity) = activity.nfcManager.defaultAdapter?.let {
        it.enableForegroundDispatch(activity,
                PendingIntent.getActivity(activity, 0, activity.intentFor<MainActivity>(), 0),
                arrayOf(IntentFilter().apply {
                    addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
                    addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
                }), arrayOf(arrayOf(MifareClassic::class.java.name)))
        true
    } ?: false


    fun disableForegroundDispatch(activity: MainActivity) = activity.nfcManager.defaultAdapter?.disableForegroundDispatch(activity)

    data class CardContent(val matriculationNumber: Int, val credit: Float, val validFrom: Date, val validUntil: Date, val lastDespositDate: Date, val lastDespositAmount: Float)

    enum class CardError(@StringRes val stringId: Int) {
        NFC_NOT_SUPPORTED(R.string.error_nfc_not_supported),
        MIFARE_NOT_SUPPORTED_OR_WRONG_TAG(R.string.error_mifare_not_supported_or_wrong_tag),
        WRONG_TAG(R.string.error_wrong_tag),
        UNKNOWN(R.string.error_unknown)
    }

    interface CardCallback {

        fun onSuccess(content: CardContent)

        fun onError(error: CardError)
    }

    fun ByteArray.toIntFromBCD(range: IntRange): Int {
        var out = 0
        var i = range.last
        var current = 1;
        while (i >= range.first) {
            out += (this[i].toUByte().toInt().and(0x0F)) * current;
            current *= 10;
            out += (this[i].toUByte().toInt().shr(4)) * current;
            current *= 10;
            i--
        }
        return out
    }

    fun handleTag(tag: MifareClassic, callback: CardCallback) {
        doAsync {
            try {
                tag.connect()

                //Read Credit
                tag.authenticateSectorWithKeyA(1,
                        byteArrayOf(0xac.toByte(), 0x87.toByte(), 0xbe.toByte(), 0x83.toByte(), 0x92.toByte(), 0x4e.toByte()))

                val blockCredit = tag.readBlock(4)
                val credit = (blockCredit[0].toUByte().toInt() or (blockCredit[1].toUByte().toInt() shl 8)) / 100.0f

                //Read MatriculationNumber, ValidFrom, Valid Until
                tag.authenticateSectorWithKeyA(3,
                        byteArrayOf(0xcd.toByte(), 0xee.toByte(), 0x63.toByte(), 0x1b.toByte(), 0xb3.toByte(), 0x7e.toByte()))

                val blockValidFrom = tag.readBlock(14)
                val validFrom = Date(blockValidFrom.toIntFromBCD(2..3) - 1900,
                        blockValidFrom.toIntFromBCD(1..1) - 1,
                        blockValidFrom.toIntFromBCD(0..0))

                val blockValidUntil = tag.readBlock(12)
                val validUntil = Date(blockValidUntil.toIntFromBCD(2..3) - 1900,
                        blockValidUntil.toIntFromBCD(1..1) - 1,
                        blockValidUntil.toIntFromBCD(0..0))

                val blockMatriculationNumber = tag.readBlock(13)
                val matriculationNumber = blockMatriculationNumber.toIntFromBCD(1..6)

                //Read Transactions
                tag.authenticateSectorWithKeyA(2,
                        byteArrayOf(0xf3.toByte(), 0x81.toByte(), 0x8f.toByte(), 0xa2.toByte(), 0x31.toByte(), 0xd6.toByte()))
                val blockLastDeposit = tag.readBlock(8)

                val lastDepositDate = Date((blockLastDeposit[4].toUByte().toInt() shl 8 or blockLastDeposit[5].toUByte().toInt()) - 1900,
                        blockLastDeposit[3] - 1,
                        blockLastDeposit[2].toInt(),
                        blockLastDeposit[6].toInt(),
                        blockLastDeposit[7].toInt())
                val lastDepositAmount = (blockLastDeposit[10].toUByte().toInt() shl 8 or blockLastDeposit[11].toUByte().toInt()) / 100.0f


                Log.d("Deposit", "D" + lastDepositDate + ", " + lastDepositAmount)
                uiThread {
                    callback.onSuccess(CardContent(matriculationNumber, credit, validFrom, validUntil, lastDepositDate, lastDepositAmount))
                }
            } catch (e: IOException) {
                e.printStackTrace()
                uiThread {
                    callback.onError(CardManager.CardError.UNKNOWN)
                }
            } finally {
                tag.close()
            }

            Unit
        }
    }
}