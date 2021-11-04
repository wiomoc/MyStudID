package de.wiomoc.mystudid.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.databinding.FragmentLoginDialogBinding
import de.wiomoc.mystudid.services.OnlineCardClient
import org.jetbrains.anko.find

class LoginDialogFragment : DialogFragment() {

    val ARG_CARD_NUMBER = "I"

    interface LoginDialogCallback {
        fun onLoginAction(credentials: OnlineCardClient.LoginCredentials)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = FragmentLoginDialogBinding.inflate(layoutInflater).apply {
            requireArguments().get(ARG_CARD_NUMBER)?.let {
                teCardNumber.text = Editable.Factory.getInstance().newEditable(it.toString())
                tePassword.requestFocus()
            }
        }.root
        return AlertDialog.Builder(requireActivity())
            .setView(view)
            .setPositiveButton("Login", null)
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onResume() {
        super.onResume()

        val positiveButton = (dialog as AlertDialog).getButton(Dialog.BUTTON_POSITIVE)
        val cardNumberTextEdit = dialog!!.find<TextView>(R.id.te_card_number)
        val passwordTextEdit = dialog!!.find<TextView>(R.id.te_password)

        positiveButton.isEnabled = false

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                positiveButton.isEnabled = cardNumberTextEdit.text.isNotEmpty() && passwordTextEdit.text.isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        cardNumberTextEdit.addTextChangedListener(watcher)
        passwordTextEdit.addTextChangedListener(watcher)

        positiveButton.setOnClickListener {
            (activity as? LoginDialogCallback)?.onLoginAction(
                OnlineCardClient.LoginCredentials(
                    cardNumberTextEdit.text.toString(),
                    passwordTextEdit.text.toString()
                )
            )
        }
    }

    companion object {
        fun FragmentActivity.showLoginDialog(cardNumber: Int?) = LoginDialogFragment().apply {
            arguments = bundleOf(ARG_CARD_NUMBER to cardNumber)
        }.show(supportFragmentManager, "Login")

        fun FragmentActivity.closeLoginDialog() =
            (supportFragmentManager.findFragmentByTag("Login") as? LoginDialogFragment)?.dismiss()
    }
}
