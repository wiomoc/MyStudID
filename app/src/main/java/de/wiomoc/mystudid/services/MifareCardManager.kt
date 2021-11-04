package de.wiomoc.mystudid.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Parcelable
import androidx.annotation.StringRes
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.activities.MainActivity
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.nfcManager
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.util.*

object MifareCardManager {

    fun enableForegroundDispatch(activity: MainActivity) = activity.nfcManager.defaultAdapter?.let {
        it.enableForegroundDispatch(activity,
            PendingIntent.getActivity(
                activity, 0, activity.intentFor<MainActivity>(),
                PendingIntent.FLAG_MUTABLE
            ),
            arrayOf(IntentFilter().apply {
                addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
                addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            }), arrayOf(arrayOf(MifareClassic::class.java.name))
        )
        true
    } ?: false


    fun disableForegroundDispatch(activity: MainActivity) = activity.nfcManager.defaultAdapter?.disableForegroundDispatch(activity)

    fun subscribeNfcStatusChanges(context: Context, callback: (nfcEnabled: Boolean) -> Unit): BroadcastReceiver? {
        callback(context.nfcManager.defaultAdapter?.isEnabled ?: false)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    callback(context?.nfcManager?.defaultAdapter?.isEnabled ?: false)
                }
            }

            context.registerReceiver(
                broadcastReceiver,
                IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            )
            broadcastReceiver
        } else {
            null
        }
    }

    fun unsubscribeNfcStatusChanges(context: Context, broadcastReceiver: BroadcastReceiver?) {
        broadcastReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
            }
        }
    }

    @Parcelize
    data class CardContent(
        val cardNumber: Int,
        val matriculationNumber: Int,
        val credit: Float,
        val validFrom: Date,
        val validUntil: Date,
        val lastDespositDate: Date?,
        val lastDespositAmount: Float?,
        val lastWithdrawDate: Date?,
        val transactionCount: Int
    ) : Parcelable

    @Parcelize
    enum class CardError(@StringRes val stringId: Int) : Parcelable {
        NFC_NOT_SUPPORTED(R.string.error_nfc_not_supported),
        MIFARE_NOT_SUPPORTED_OR_WRONG_TAG(R.string.error_mifare_not_supported_or_wrong_tag),
        WRONG_TAG(R.string.error_wrong_tag),
        UNKNOWN(R.string.error_unknown)
    }

    interface CardCallback {

        fun onSuccess(content: CardContent)

        fun onError(error: CardError, cardNumber: Int?)
    }

    fun ByteArray.toIntFromBCD(range: IntRange): Int {
        var out = 0
        var i = range.last
        var current = 1
        while (i >= range.first) {
            out += (this[i].toUByte().toInt().and(0x0F)) * current
            current *= 10
            out += (this[i].toUByte().toInt().shr(4)) * current
            current *= 10
            i--
        }
        return out
    }

    private fun ByteArray.toInt() =
        ((this[3].toInt() and 0xFF) shl 24) or
                ((this[2].toInt() and 0xFF) shl 16) or
                ((this[1].toInt() and 0xFF) shl 8) or
                (this[0].toInt() and 0xFF)


    fun handleTag(tag: Tag, callback: CardCallback) {
        val cardNumber = tag.id.toInt()

        MifareClassic.get(tag)?.let { mifareTag ->
            doAsync {
                try {
                    mifareTag.connect()

                    //Read Credit
                    mifareTag.authenticateSectorWithKeyA(
                        1,
                        byteArrayOf(
                            0xac.toByte(),
                            0x87.toByte(),
                            0xbe.toByte(),
                            0x83.toByte(),
                            0x92.toByte(),
                            0x4e.toByte()
                        )
                    )

                    val blockCredit = mifareTag.readBlock(4)
                    val credit = (blockCredit[0].toUByte().toInt() or (blockCredit[1].toUByte().toInt() shl 8)) / 100.0f

                    //Read MatriculationNumber, ValidFrom, Valid Until
                    mifareTag.authenticateSectorWithKeyA(
                        3,
                        byteArrayOf(
                            0xcd.toByte(),
                            0xee.toByte(),
                            0x63.toByte(),
                            0x1b.toByte(),
                            0xb3.toByte(),
                            0x7e.toByte()
                        )
                    )

                    val blockValidFrom = mifareTag.readBlock(14)
                    val validFrom = Date(
                        blockValidFrom.toIntFromBCD(2..3) - 1900,
                        blockValidFrom.toIntFromBCD(1..1) - 1,
                        blockValidFrom.toIntFromBCD(0..0)
                    )

                    val blockValidUntil = mifareTag.readBlock(12)
                    val validUntil = Date(
                        blockValidUntil.toIntFromBCD(2..3) - 1900,
                        blockValidUntil.toIntFromBCD(1..1) - 1,
                        blockValidUntil.toIntFromBCD(0..0)
                    )

                    val blockMatriculationNumber = mifareTag.readBlock(13)
                    val matriculationNumber = blockMatriculationNumber.toIntFromBCD(1..6)

                    //Read Transactions
                    mifareTag.authenticateSectorWithKeyA(
                        2,
                        byteArrayOf(
                            0xf3.toByte(),
                            0x81.toByte(),
                            0x8f.toByte(),
                            0xa2.toByte(),
                            0x31.toByte(),
                            0xd6.toByte()
                        )
                    )
                    val blockLastDeposit = mifareTag.readBlock(8)

                    val lastDepositDate = Date(
                        (blockLastDeposit[4].toUByte().toInt() shl 8 or blockLastDeposit[5].toUByte().toInt()) - 1900,
                        blockLastDeposit[3] - 1,
                        blockLastDeposit[2].toInt(),
                        blockLastDeposit[6].toInt(),
                        blockLastDeposit[7].toInt()
                    )
                    val lastDepositAmount =
                        (blockLastDeposit[10].toUByte().toInt() shl 8 or blockLastDeposit[11].toUByte()
                            .toInt()) / 100.0f

                    val blockLastWithdraw = mifareTag.readBlock(9)

                    val lastWithdrawDateDate = Date(
                        (blockLastWithdraw[4].toUByte().toInt() shl 8 or blockLastWithdraw[5].toUByte().toInt()) - 1900,
                        blockLastWithdraw[3] - 1,
                        blockLastWithdraw[2].toInt(),
                        blockLastWithdraw[6].toInt(),
                        blockLastWithdraw[7].toInt()
                    )

                    val transactionCount = blockLastWithdraw[13].toInt() shl 8 or blockLastWithdraw[14].toInt()

                    if (PreferencesManager.cardNumber == null) {
                        PreferencesManager.cardNumber = cardNumber
                    }

                    val cardContent = CardContent(
                        cardNumber, matriculationNumber, credit, validFrom,
                        validUntil, lastDepositDate, lastDepositAmount, lastWithdrawDateDate, transactionCount
                    )

                    uiThread {
                        callback.onSuccess(cardContent)
                    }

                    if (PreferencesManager.cardNumber == cardNumber) {
                        HistoryManager.addCardTransaction(cardContent)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    uiThread {
                        callback.onError(CardError.UNKNOWN, cardNumber)
                    }
                } finally {
                    mifareTag.close()
                }

                Unit
            }
        } ?: callback.onError(CardError.MIFARE_NOT_SUPPORTED_OR_WRONG_TAG, cardNumber)

    }
}
