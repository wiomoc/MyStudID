package de.wiomoc.mystudid.activities

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.wiomoc.mystudid.fragments.CardContentFragment
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.fragments.CardErrorFragment
import de.wiomoc.mystudid.fragments.StartFragment
import de.wiomoc.mystudid.services.CardManager
import java.util.*

class MainActivity : AppCompatActivity(), CardManager.CardCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) {
            fragmentManager.beginTransaction()
                    .replace(R.id.activity_main_fragment_container, StartFragment())
                    .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!CardManager.enableForegroundDispatch(this)) {
            onError(CardManager.CardError.NFC_NOT_SUPPORTED)
        }
    }

    override fun onPause() {
        super.onPause()
        CardManager.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag.techList.contains(MifareClassic::javaClass.name)) {
                CardManager.handleTag(MifareClassic.get(tag), this)
                return
            } else {
                onError(CardManager.CardError.MIFARE_NOT_SUPPORTED_OR_WRONG_TAG)
            }
        }
        super.onNewIntent(intent)
    }

    override fun onSuccess(content: CardManager.CardContent) {
        fragmentManager.beginTransaction()
                .replace(R.id.activity_main_fragment_container, CardContentFragment(content))
                .commit()
    }

    override fun onError(error: CardManager.CardError) {
        fragmentManager.beginTransaction()
                .replace(R.id.activity_main_fragment_container, CardErrorFragment(error))
                .commit()
    }
}
