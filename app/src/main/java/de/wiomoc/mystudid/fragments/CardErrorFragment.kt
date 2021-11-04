package de.wiomoc.mystudid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import de.wiomoc.mystudid.databinding.FragmentCardErrorBinding
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.showLoginDialog
import de.wiomoc.mystudid.services.MifareCardManager

class CardErrorFragment : Fragment() {

    val ARG_CARD_ERROR = "C"
    val ARG_CARD_NUMBER = "I"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentCardErrorBinding.inflate(inflater, container, false).apply {
            tvErrorDescription.setText(requireArguments().getParcelable<MifareCardManager.CardError>(ARG_CARD_ERROR)!!.stringId)
            buttonAddAccount.setOnClickListener {
                requireActivity().showLoginDialog(requireArguments().getInt(ARG_CARD_NUMBER))
            }
        }.root


    companion object {
        fun newInstance(error: MifareCardManager.CardError, cardNumber: Int?) = CardErrorFragment().apply {
            arguments = bundleOf(ARG_CARD_ERROR to error, ARG_CARD_NUMBER to cardNumber)
        }
    }
}
