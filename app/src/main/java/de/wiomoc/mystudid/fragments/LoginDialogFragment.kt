package de.wiomoc.mystudid.fragments


import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.services.OnlineCardClient
import org.jetbrains.anko.find


class LoginDialogFragment : DialogFragment() {

    interface LoginDialogCallback {
        fun onLoginAction(credentials: OnlineCardClient.LoginCredentials)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val view = activity!!.layoutInflater.inflate(R.layout.fragment_login_dialog, null)
        builder.setView(view)
                .setPositiveButton("Login") { dialog, id ->
                    (activity as? LoginDialogCallback)?.onLoginAction(OnlineCardClient.LoginCredentials(
                            view.find<TextView>(R.id.te_card_number).text.toString(),
                            view.find<TextView>(R.id.te_password).text.toString()))
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
        return builder.create()
    }

    companion object {
        fun FragmentActivity.showLoginDialog() = LoginDialogFragment().show(supportFragmentManager, "Login")
    }
}
