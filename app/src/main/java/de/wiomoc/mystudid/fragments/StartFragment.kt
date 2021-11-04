package de.wiomoc.mystudid.fragments

import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.databinding.FragmentStartBinding
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.showLoginDialog
import de.wiomoc.mystudid.services.MifareCardManager
import de.wiomoc.mystudid.services.PreferencesManager
import org.jetbrains.anko.find

class StartFragment : Fragment() {

    var nfcStatusChangeReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentStartBinding.inflate(inflater, container, false).apply {
        buttonAddAccount.setOnClickListener {
            requireActivity().showLoginDialog(PreferencesManager.cardNumber)
        }
    }.root

    override fun onStart() {
        super.onStart()
        nfcStatusChangeReceiver = MifareCardManager.subscribeNfcStatusChanges(requireContext()) { enabled ->
            view?.find<TextView>(R.id.label_start_guideline)?.setText(
                if (enabled) R.string.start_guideline else R.string.start_guideline_nfc_disabled
            )
        }
    }

    override fun onPause() {
        super.onPause()
        MifareCardManager.unsubscribeNfcStatusChanges(requireContext(), nfcStatusChangeReceiver)
    }

}
