package de.wiomoc.mystudid.services

import android.app.PendingIntent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.MifareClassic
import android.support.annotation.StringRes
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
                arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)), arrayOf(arrayOf(MifareClassic::class.java.name)))
        true
    } ?: false


    fun disableForegroundDispatch(activity: MainActivity) = activity.nfcManager.defaultAdapter?.disableForegroundDispatch(activity)

    data class CardContent(val matriculationNumber: Int, val credit: Float, val validFrom: Date, val validUntil: Date)

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
            out += (this[i].toInt().and(0x0F)) * current;
            current *= 10;
            out += (this[i].toInt().shr(4)) * current;
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
                val credit = (blockCredit[0].toInt() and (blockCredit[1].toInt() shl 8)) / 1000.0f

                //Read MatriculationNumber, ValidFrom, Valid Until
                tag.authenticateSectorWithKeyA(3,
                        byteArrayOf(0xcd.toByte(), 0xee.toByte(), 0x63.toByte(), 0x1b.toByte(), 0xb3.toByte(), 0x7e.toByte()))

                val blockValidFrom = tag.readBlock(12)
                val validFrom = Date(blockValidFrom.toIntFromBCD(2..3),
                        blockValidFrom.toIntFromBCD(1..1),
                        blockValidFrom.toIntFromBCD(0..0))

                val blockValidUntil = tag.readBlock(14)
                val validUntil = Date(blockValidUntil.toIntFromBCD(2..3),
                        blockValidUntil.toIntFromBCD(1..1),
                        blockValidUntil.toIntFromBCD(0..0))

                val blockMatriculationNumber = tag.readBlock(13)
                val matriculationNumber = blockMatriculationNumber.toIntFromBCD(1..6)

                uiThread {
                    callback.onSuccess(CardContent(matriculationNumber, credit, validFrom, validUntil))
                }
            } catch (e: IOException) {
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