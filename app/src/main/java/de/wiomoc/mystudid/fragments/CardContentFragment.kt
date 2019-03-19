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
import java.text.SimpleDateFormat

class CardContentFragment : Fragment() {

    val ARG_CARD_CONTENT = "C"

    val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
    val dateTimeFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            arguments?.getParcelable<MifareCardManager.CardContent>(ARG_CARD_CONTENT)?.let { cardContent ->
                inflater.inflate(R.layout.fragment_card_content, container, false).apply {
                    find<TextView>(R.id.tv_available_credit).text = String.format("%.2f€", cardContent.credit)
                    find<TextView>(R.id.tv_matriculation_number).text = cardContent.matriculationNumber.toString()
                    find<TextView>(R.id.tv_valid_from).text = dateFormatter.format(cardContent.validFrom)
                    find<TextView>(R.id.tv_valid_until).text = dateFormatter.format(cardContent.validUntil)
                    find<TextView>(R.id.tv_valid_until).error = "Läuft bald ab"
                    find<TextView>(R.id.tv_valid_until).requestFocus()
                    find<TextView>(R.id.tv_last_deposite_date).text = dateTimeFormatter.format(cardContent.lastDespositDate)
                    find<TextView>(R.id.tv_last_deposit_amount).text = String.format("%.2f€", cardContent.lastDespositAmount)
                }
            }

    companion object {
        fun newInstance(content: MifareCardManager.CardContent) = CardContentFragment().apply {
            arguments = bundleOf(ARG_CARD_CONTENT to content)
        }
    }

}
