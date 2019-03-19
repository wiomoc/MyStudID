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

    var loginCredentials: OnlineCardClient.LoginCredentials?
        get() = sharedPreferences?.getString("cardNumber", null)?.let { cardNumber ->
            OnlineCardClient.LoginCredentials(cardNumber, sharedPreferences!!.getString("password", null)!!)
        }
        set(value) {
            sharedPreferences?.edit {
                putString("cardNumber", value?.cardNumber)
                putString("password", value?.password)
            }
        }

}