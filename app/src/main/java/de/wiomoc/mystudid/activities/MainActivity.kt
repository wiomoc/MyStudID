package de.wiomoc.mystudid.activities

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.transaction
import de.wiomoc.mystudid.fragments.CardContentFragment
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.database.HistoryDatabase
import de.wiomoc.mystudid.fragments.CardErrorFragment
import de.wiomoc.mystudid.fragments.LoginDialogFragment
import de.wiomoc.mystudid.fragments.StartFragment
import de.wiomoc.mystudid.services.MifareCardManager
import de.wiomoc.mystudid.services.OnlineCardClient
import de.wiomoc.mystudid.services.PreferencesManager
import org.jetbrains.anko.intentFor

class MainActivity : AppCompatActivity(), MifareCardManager.CardCallback, LoginDialogFragment.LoginDialogCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        HistoryDatabase.init(this)
        setContentView(R.layout.activity_main)
        if (!handleIntent(intent) && savedInstanceState == null) {
            supportFragmentManager.transaction {
                replace(R.id.activity_main_fragment_container, StartFragment())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!MifareCardManager.enableForegroundDispatch(this)) {
            onError(MifareCardManager.CardError.NFC_NOT_SUPPORTED)
        }
    }

    override fun onPause() {
        super.onPause()
        MifareCardManager.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        if (!handleIntent(intent)) {
            super.onNewIntent(intent)
        }
    }

    fun handleIntent(intent: Intent) =
            if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED || intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
                val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                MifareClassic.get(tag)?.let {
                    MifareCardManager.handleTag(it, this)
                } ?: onError(MifareCardManager.CardError.MIFARE_NOT_SUPPORTED_OR_WRONG_TAG)
                true
            } else {
                false
            }


    override fun onSuccess(content: MifareCardManager.CardContent) {
        supportFragmentManager.transaction {
            replace(R.id.activity_main_fragment_container, CardContentFragment.newInstance(content))
        }
    }

    override fun onError(error: MifareCardManager.CardError) {
        supportFragmentManager.transaction {
            replace(R.id.activity_main_fragment_container, CardErrorFragment.newInstance(error))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_history).isVisible = (PreferencesManager.loginCredentials != null)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(intentFor<HistoryActivity>())
                true
            }
            R.id.action_settings -> {
                startActivity(intentFor<SettingsActivity>())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLoginAction(credentials: OnlineCardClient.LoginCredentials) {
        OnlineCardClient.login(object : OnlineCardClient.ResponseCallback<OnlineCardClient.LoginResponse> {
            override fun onSuccess(response: OnlineCardClient.LoginResponse) {
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_LONG).show()
                startActivity(intentFor<HistoryActivity>())
            }

            override fun onCredentialsRequired(cb: (loginCredentials: OnlineCardClient.LoginCredentials) -> Unit) {
                Toast.makeText(this@MainActivity, "Wrong Creds", Toast.LENGTH_LONG).show()
            }

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@MainActivity, "Failture", Toast.LENGTH_LONG).show()
            }

        }, credentials)
    }
}
