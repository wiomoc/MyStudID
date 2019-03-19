package de.wiomoc.mystudid.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.services.MifareCardManager
import org.jetbrains.anko.find

class CardErrorFragment : Fragment() {

    val ARG_CARD_ERROR = "C"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            arguments?.getParcelable<MifareCardManager.CardError>(ARG_CARD_ERROR)?.let { cardError ->
                inflater.inflate(R.layout.fragment_card_error, container, false).apply {
                    find<TextView>(R.id.tv_error_description).setText(cardError.stringId)

                }
            }

    companion object {
        fun newInstance(error: MifareCardManager.CardError) = CardErrorFragment().apply {
            arguments = bundleOf(ARG_CARD_ERROR to error)
        }
    }
}
