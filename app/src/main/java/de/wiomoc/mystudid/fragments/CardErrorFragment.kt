package de.wiomoc.mystudid.fragments


import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.services.CardManager
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

class CardErrorFragment : Fragment() {

    val ARG_CARD_ERROR = "C"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater
            .inflate(R.layout.fragment_card_error, container, false).apply {
                find<TextView>(R.id.tv_error_description).setText(arguments.getParcelable<CardManager.CardError>(ARG_CARD_ERROR).stringId)
            }

    companion object {
        fun newInstance(error: CardManager.CardError) = CardErrorFragment().apply {
            arguments = bundleOf(ARG_CARD_ERROR to error)
        }
    }
}
