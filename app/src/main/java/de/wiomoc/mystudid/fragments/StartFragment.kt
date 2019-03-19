package de.wiomoc.mystudid.fragments


import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.activities.HistoryActivity
import de.wiomoc.mystudid.services.MifareCardManager
import de.wiomoc.mystudid.services.OnlineCardClient
import org.jetbrains.anko.find
import org.jetbrains.anko.intentFor
import de.wiomoc.mystudid.fragments.LoginDialogFragment
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.showLoginDialog

class StartFragment : Fragment() {

    var nfcStatusChangeReceiver: BroadcastReceiver? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?) = inflater
            .inflate(R.layout.fragment_start, container, false).apply {
                find<Button>(R.id.button_add_account).setOnClickListener {
                    activity!!.showLoginDialog()
                }
            }

    override fun onStart() {
        super.onStart()
        nfcStatusChangeReceiver = MifareCardManager.subscribeNfcStatusChanges(context!!) { enabled ->
            view?.find<TextView>(R.id.label_start_guideline)?.setText(
                    if (enabled) R.string.start_guideline else R.string.start_guideline_nfc_disabled)
        }
    }

    override fun onPause() {
        super.onPause()
        MifareCardManager.unsubscribeNfcStatusChanges(context!!, nfcStatusChangeReceiver)
    }

}
