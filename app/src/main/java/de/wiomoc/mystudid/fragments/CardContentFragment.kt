package de.wiomoc.mystudid.fragments

import android.annotation.SuppressLint
import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.services.CardManager
import org.jetbrains.anko.find

@SuppressLint("ValidFragment")
class CardContentFragment(val cardContent: CardManager.CardContent) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater
            .inflate(R.layout.fragment_card_content, container, false).apply {
                find<TextView>(R.id.tv_available_credit).text = cardContent.credit.toString() + "€"
                find<TextView>(R.id.tv_matriculation_number).text = cardContent.matriculationNumber.toString()
            }

}
