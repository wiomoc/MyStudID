package de.wiomoc.mystudid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import de.wiomoc.mystudid.databinding.FragmentCardContentBinding
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.showLoginDialog
import de.wiomoc.mystudid.services.MifareCardManager
import java.text.SimpleDateFormat

class CardContentFragment : Fragment() {

    val ARG_CARD_CONTENT = "C"

    private val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
    private val dateTimeFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        arguments?.getParcelable<MifareCardManager.CardContent>(ARG_CARD_CONTENT)?.let { cardContent ->
            FragmentCardContentBinding.inflate(inflater, container, false).apply {
                tvAvailableCredit.text = String.format("%.2f€", cardContent.credit)
                tvCardNumber.text = cardContent.cardNumber.toString()
                tvMatriculationNumber.text = cardContent.matriculationNumber.toString()
                tvValidFrom.text = dateFormatter.format(cardContent.validFrom)
                tvValidUntil.text = dateFormatter.format(cardContent.validUntil)
                //find<TextView>(R.cardNumber.tv_valid_until).error = "Läuft bald ab"
                //find<TextView>(R.cardNumber.tv_valid_until).requestFocus()
                tvLastDepositeDate.text = dateTimeFormatter.format(cardContent.lastDespositDate)
                tvLastDepositAmount.text = String.format("%.2f€", cardContent.lastDespositAmount)
                buttonAddAccount.setOnClickListener {
                    requireActivity().showLoginDialog(cardContent.cardNumber)
                }
            }.root
        }

    companion object {
        fun newInstance(content: MifareCardManager.CardContent) = CardContentFragment().apply {
            arguments = bundleOf(ARG_CARD_CONTENT to content)
        }
    }

}
