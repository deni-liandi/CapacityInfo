package com.ph03nix_x.capacityinfo.fragments

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.ph03nix_x.capacityinfo.MainApp.Companion.isGooglePlay
import com.ph03nix_x.capacityinfo.MainApp.Companion.isInstalledGooglePlay
import com.ph03nix_x.capacityinfo.R
import com.ph03nix_x.capacityinfo.utilities.Constants.GOOGLE_PLAY_APP_LINK
import com.ph03nix_x.capacityinfo.utilities.Constants.TELEGRAM_DEVELOPER_LINK
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.IS_FORCIBLY_SHOW_RATE_THE_APP

class FeedbackFragment : PreferenceFragmentCompat() {


    // Telegram
    private var telegramDeveloper: Preference? = null

    // Other
    private var email: Preference? = null
    private var rateTheApp: Preference? = null
    private var shareTheApp: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

       val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        addPreferencesFromResource(R.xml.feedback_settings)


        // Telegram
        telegramDeveloper = findPreference("telegram_developer")

        telegramDeveloper?.setOnPreferenceClickListener {

            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_DEVELOPER_LINK))) }

            catch(e: ActivityNotFoundException) {

                val clipboardManager = requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("telegram_developer",
                    TELEGRAM_DEVELOPER_LINK)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(requireContext(), R.string.telegram_link_copied,
                    Toast.LENGTH_LONG).show()
            }

            true
        }

        // Other
        email = findPreference("email")

        rateTheApp = findPreference("rate_the_app")

        shareTheApp = findPreference("share_the_app")

        email?.setOnPreferenceClickListener {

            try {

                val version = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    requireContext().packageManager?.getPackageInfo(requireContext().packageName,
                        PackageManager.PackageInfoFlags.of(0))?.versionName
                else {
                    requireContext().packageManager?.getPackageInfo(requireContext().packageName,
                        0)?.versionName
                }
                val build = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    requireContext().packageManager?.getPackageInfo(requireContext().packageName,
                        PackageManager.PackageInfoFlags.of(0))?.let {
                        PackageInfoCompat.getLongVersionCode(it).toString()
                    }
                else {
                    requireContext().packageManager?.getPackageInfo(requireContext().packageName,
                        0)?.let { PackageInfoCompat.getLongVersionCode(it).toString() }
                }
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:${email
                    ?.summary}?subject=Capacity Info $version (Build $build). ${requireContext().getString(R.string.feedback)}")))
            }

            catch(e: ActivityNotFoundException) {

                val clipboardManager = requireContext().getSystemService(
                    Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("email", email?.summary)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(requireContext(), R.string.email_copied,
                    Toast.LENGTH_LONG).show()
            }

            true
        }

        rateTheApp?.apply {
            isVisible = isInstalledGooglePlay && isGooglePlay(requireContext()) || pref.getBoolean(
                IS_FORCIBLY_SHOW_RATE_THE_APP, resources.getBoolean(
                    R.bool.is_forcibly_show_rate_the_app))

            setOnPreferenceClickListener {

                try {
                    requireContext().startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(GOOGLE_PLAY_APP_LINK)))
                }
                catch(e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), requireContext().getString(
                        R.string.unknown_error), Toast.LENGTH_LONG).show()
                }

                true
            }
        }

        shareTheApp?.setOnPreferenceClickListener {

            val linkToGooglePlay = GOOGLE_PLAY_APP_LINK

            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, linkToGooglePlay)

            }, getString(R.string.share_the_app)))

            true
        }
    }
}