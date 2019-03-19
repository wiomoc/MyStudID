package de.wiomoc.mystudid.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import de.wiomoc.mystudid.R
import org.jetbrains.anko.intentFor


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, PreferenceFragment())
                .commit()

    }


    class PreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference("licenses").setOnPreferenceClickListener {
                startActivity(activity!!.intentFor<OssLicensesMenuActivity>())
                true
            }
        }
    }
}
