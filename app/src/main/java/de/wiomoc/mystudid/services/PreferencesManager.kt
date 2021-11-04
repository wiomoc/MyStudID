package de.wiomoc.mystudid.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.jetbrains.anko.defaultSharedPreferences

object PreferencesManager {
    var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.defaultSharedPreferences
    }

    var cardNumber: Int?
        get() = sharedPreferences?.getString("cardNumber", null)?.toInt()
        set(value) {
            sharedPreferences?.edit {
                putString("cardNumber", value.toString())
            }
        }

    var loginCredentials: OnlineCardClient.LoginCredentials?
        get() = sharedPreferences?.getString("cardNumber", null)?.let { cardNumber ->
            sharedPreferences!!.getString("password", null)?.let { password ->
                OnlineCardClient.LoginCredentials(cardNumber, password)
            }
        }
        set(value) {
            sharedPreferences?.edit {
                putString("cardNumber", value?.cardNumber)
                putString("password", value?.password)
            }
        }

}
