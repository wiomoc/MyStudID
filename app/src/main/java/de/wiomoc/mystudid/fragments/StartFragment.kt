package de.wiomoc.mystudid.fragments


import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import de.wiomoc.mystudid.R

class StartFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?) = inflater
            .inflate(R.layout.fragment_start, container, false)

}
