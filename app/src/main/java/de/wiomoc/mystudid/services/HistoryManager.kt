package de.wiomoc.mystudid.services

import de.wiomoc.mystudid.database.HistoryDatabase
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*
import kotlin.math.absoluteValue

object HistoryManager {
    fun syncDatabase(callback: OnlineCardClient.ResponseCallback<Boolean>) {
        doAsync {
            val lastSync = HistoryDatabase.dao.getLastOnlineTransactionDate() ?: {
                val gregorianCalendar = GregorianCalendar()
                gregorianCalendar.add(GregorianCalendar.DATE, -180)
                Date(gregorianCalendar.timeInMillis)
            }()

            OnlineCardClient.transactions(object : OnlineCardClient.ResponseCallback<Array<OnlineCardClient.Transaction>> {
                override fun onSuccess(response: Array<OnlineCardClient.Transaction>) {

                    if (response.isNotEmpty()) {
                        doAsync {
                            val searchDate = Date(response.first().date.time - 20 * 60 * 1000)

                            val transactionBefore = HistoryDatabase.dao.getTransactionBefore(searchDate)
                            val cardTransactions = HistoryDatabase.dao.getAllCardTransactionsAfter(searchDate)

                            val onlineTransactions = response.map {
                                HistoryDatabase.Transaction(
                                    date = it.date,
                                    amount = it.amount,
                                    location = it.location,
                                    onlineID = it.id
                                )
                            }.sortedBy { it.date }

                            onlineTransactions.forEachIndexed { index, onlineTransaction ->
                                var matchedCardTransaction =
                                    cardTransactions.find { it.date.approximatelyEquals(onlineTransaction.date) && it.amount == onlineTransaction.amount }

                                if (matchedCardTransaction != null) {
                                    HistoryDatabase.dao.deleteTransaction(matchedCardTransaction)
                                } else if (transactionBefore != null
                                    && transactionBefore.date.approximatelyEquals(onlineTransaction.date)
                                    && transactionBefore.amount == onlineTransaction.amount
                                ) {
                                    matchedCardTransaction = transactionBefore
                                }

                                if (matchedCardTransaction?.credit != null) {
                                    var current = matchedCardTransaction.credit!!

                                    onlineTransactions.take(index).reversed().forEach {
                                        it.credit = current
                                        current -= it.amount!!
                                    }

                                    current = matchedCardTransaction.credit!!

                                    onlineTransactions.slice(index until onlineTransactions.size).forEach {
                                        it.credit = current
                                        current += it.amount!!
                                    }

                                }

                            }

                            HistoryDatabase.dao.insertOnlineTransactions(onlineTransactions)

                            uiThread {
                                callback.onSuccess(true)
                            }
                        }

                    }

                }

                override fun onCredentialsRequired(cb: (loginCredentials: OnlineCardClient.LoginCredentials) -> Unit) {
                    callback.onCredentialsRequired(cb)
                }

                override fun onFailure(t: Throwable) {
                    callback.onFailure(t)
                }

            }, lastSync)
        }
    }

    fun addCardTransaction(cardContent: MifareCardManager.CardContent) {
        val lastTransaction = HistoryDatabase.dao.getLastTransaction()

        println(cardContent)
        println(lastTransaction)

        if (cardContent.lastDespositDate != null && (cardContent.lastWithdrawDate == null ||
                    cardContent.lastDespositDate > cardContent.lastWithdrawDate)
        ) {
            if (lastTransaction != null
                && lastTransaction.date.approximatelyEquals(cardContent.lastDespositDate)
                && lastTransaction.amount == cardContent.lastDespositAmount
            ) {
                if (lastTransaction.credit != cardContent.credit) {
                    var currentCredit = cardContent.credit
                    HistoryDatabase.dao.updateTransactions(
                        HistoryDatabase.dao.getAllTransactionsWithoutCredit().map {
                            it.credit = currentCredit
                            currentCredit -= it.amount!!
                            it
                        }
                    )
                }
            } else {
                HistoryDatabase.dao.insertOnlineTransactions(
                    listOf(
                        HistoryDatabase.Transaction(
                            cardID = cardContent.transactionCount,
                            amount = cardContent.lastDespositAmount!!,
                            date = cardContent.lastDespositDate,
                            credit = cardContent.credit
                        )
                    )
                )
            }
        } else if (cardContent.lastWithdrawDate != null && lastTransaction != null
            && cardContent.lastWithdrawDate.approximatelyEquals(lastTransaction.date)
            && lastTransaction.amount!! < 0
        ) {
            var currentCredit = cardContent.credit
            HistoryDatabase.dao.updateTransactions(
                HistoryDatabase.dao.getAllTransactionsWithoutCredit().map {
                    it.credit = currentCredit
                    currentCredit -= it.amount!!
                    it
                }
            )
        }
    }

    fun Date.approximatelyEquals(other: Date) = (this.time - other.time).absoluteValue < 1000 * 60 * 20

    fun hasTransactionsSaved() = HistoryDatabase.dao.hasTransactions() == 1

    fun deleteAllTransactions() = HistoryDatabase.dao.deleteAllTransactions()
}
