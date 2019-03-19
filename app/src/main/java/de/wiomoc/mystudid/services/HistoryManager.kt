package de.wiomoc.mystudid.services

import de.wiomoc.mystudid.database.HistoryDatabase
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

object HistoryManager {
    fun syncDatabase(callback: OnlineCardClient.ResponseCallback<Boolean>) {
        val currentDate = Date()

        doAsync {
            val lastSync = HistoryDatabase.dao.getLastOnlineTransactionDate() ?: {
                val gregorianCalendar = GregorianCalendar()
                gregorianCalendar.add(GregorianCalendar.DATE, -180)
                Date(gregorianCalendar.timeInMillis)
            }()

            OnlineCardClient.transactions(object : OnlineCardClient.ResponseCallback<Array<OnlineCardClient.Transaction>> {
                override fun onSuccess(response: Array<OnlineCardClient.Transaction>) {
                    doAsync {
                        HistoryDatabase.dao.insertOnlineTransaction(response.map {
                            HistoryDatabase.Transaction(date = it.date, amount = it.amount, location = it.location, onlineID = it.id)
                        })

                        uiThread {
                            callback.onSuccess(true)
                        }
                    }

                }

                override fun onCredentialsRequired(cb: (loginCredentials: OnlineCardClient.LoginCredentials) -> Unit) {
                    callback.onCredentialsRequired(cb)
                }

                override fun onFailure(t: Throwable) {
                    callback.onFailure(t)
                }

            }, lastSync, currentDate)
        }
    }
}
