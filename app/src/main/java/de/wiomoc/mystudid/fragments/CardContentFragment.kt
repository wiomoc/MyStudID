package de.wiomoc.mystudid.fragments

import android.annotation.SuppressLint
import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.services.CardManager
import org.jetbrains.anko.find
import java.text.SimpleDateFormat

@SuppressLint("ValidFragment")
class CardContentFragment(val cardContent: CardManager.CardContent) : Fragment() {

    val dateFormatter = SimpleDateFormat("dd.MM.yyyy")
    val dateTimeFormatter = SimpleDateFormat("dd.MM.yyyy HH:MM")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater
            .inflate(R.layout.fragment_card_content, container, false).apply {
                find<TextView>(R.id.tv_available_credit).text = String.format("%.2f€", cardContent.credit)
                find<TextView>(R.id.tv_matriculation_number).text = cardContent.matriculationNumber.toString()
                find<TextView>(R.id.tv_valid_from).text = dateFormatter.format(cardContent.validFrom)
                find<TextView>(R.id.tv_valid_until).text = dateFormatter.format(cardContent.validUntil)
                find<TextView>(R.id.tv_last_deposite_date).text = dateTimeFormatter.format(cardContent.lastDespositDate)
                find<TextView>(R.id.tv_last_deposit_amount).text = String.format("%.2f€", cardContent.lastDespositAmount)
            }

}
