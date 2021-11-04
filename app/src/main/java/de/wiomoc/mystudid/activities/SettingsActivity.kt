package de.wiomoc.mystudid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.services.HistoryManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.InputStreamReader

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, PreferenceFragment())
            .commit()

    }

    data class Libraries(@SerializedName("libraries") val libraries: Array<Library>)
    data class Library(
        @SerializedName("libraryName") val libraryName: String,
        @SerializedName("license") val license: String,
        @SerializedName("licenseUrl") val licenseUrl: String?,
        @SerializedName("copyrightStatement") val copyrightStatement: String
    )

    class PreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val deleteTransactionHistoryPreference = findPreference<Preference>("deleteTransactionHistory")!!

            doAsync {
                if (!HistoryManager.hasTransactionsSaved()) {
                    uiThread {
                        deleteTransactionHistoryPreference.isEnabled = false
                    }
                }
            }

            deleteTransactionHistoryPreference.setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Really delete Transactionhistory")
                    .setPositiveButton("Yes") { _, _ ->
                        doAsync {
                            HistoryManager.deleteAllTransactions()
                        }
                        deleteTransactionHistoryPreference.isEnabled = false
                    }.setNegativeButton("No", null)
                    .create()
                    .show()

                true
            }


            val licensesPreferenceGroup = findPreference<PreferenceGroup>("licenses")!!

            val libraries = Gson().fromJson(
                InputStreamReader(requireContext().assets.open("licenses.json")),
                Libraries::class.java
            )

            for (library in libraries.libraries) {
                val licensePreference = Preference(context)
                licensePreference.title = library.libraryName
                licensePreference.summary = Html.fromHtml("${library.license}<br>${library.copyrightStatement}")
                library.licenseUrl?.let {
                    licensePreference.intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(library.licenseUrl) }
                }
                licensesPreferenceGroup.addPreference(licensePreference)
            }
        }
    }
}
