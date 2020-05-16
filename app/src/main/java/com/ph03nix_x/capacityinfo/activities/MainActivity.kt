package com.ph03nix_x.capacityinfo.activities

import android.content.*
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ph03nix_x.capacityinfo.*
import com.ph03nix_x.capacityinfo.MainApp.Companion.defLang
import com.ph03nix_x.capacityinfo.fragments.ChargeDischargeFragment
import com.ph03nix_x.capacityinfo.fragments.DebugFragment
import com.ph03nix_x.capacityinfo.fragments.SettingsFragment
import com.ph03nix_x.capacityinfo.fragments.WearFragment
import com.ph03nix_x.capacityinfo.utils.Utils.batteryIntent
import com.ph03nix_x.capacityinfo.services.*
import com.ph03nix_x.capacityinfo.view.CenteredToolbar
import com.ph03nix_x.capacityinfo.helpers.LocaleHelper
import com.ph03nix_x.capacityinfo.interfaces.BatteryInfoInterface
import com.ph03nix_x.capacityinfo.interfaces.OverlayInterface
import com.ph03nix_x.capacityinfo.interfaces.ServiceInterface
import com.ph03nix_x.capacityinfo.interfaces.SettingsInterface
import com.ph03nix_x.capacityinfo.utils.Constants.IMPORT_SETTINGS_EXTRA
import com.ph03nix_x.capacityinfo.utils.Constants.MAX_DESIGN_CAPACITY
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.BATTERY_LEVEL_TO
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.BATTERY_LEVEL_WITH
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.CAPACITY_ADDED
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.DESIGN_CAPACITY
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.IS_SHOW_INSTRUCTION
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.IS_SHOW_NOT_SUPPORTED_DIALOG
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.IS_SUPPORTED
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.LANGUAGE
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.LAST_CHARGE_TIME
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.NUMBER_OF_CHARGES
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.NUMBER_OF_CYCLES
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.PERCENT_ADDED
import com.ph03nix_x.capacityinfo.utils.PreferencesKeys.RESIDUAL_CAPACITY
import com.ph03nix_x.capacityinfo.utils.Utils.fragment
import com.ph03nix_x.capacityinfo.utils.Utils.isStartedService

class MainActivity : AppCompatActivity(), ServiceInterface, BatteryInfoInterface,
    SettingsInterface {

    lateinit var toolbar: CenteredToolbar

    lateinit var navigation: BottomNavigationView

    private lateinit var pref: SharedPreferences

    companion object {

        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        pref = PreferenceManager.getDefaultSharedPreferences(this)

        super.onCreate(savedInstanceState)

        LocaleHelper.setLocale(this, pref.getString(LANGUAGE, null) ?: defLang)

        setContentView(R.layout.activity_main)

        batteryIntent = registerReceiver(null, IntentFilter(
            Intent.ACTION_BATTERY_CHANGED))

        var status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        if(fragment == null) fragment = ChargeDischargeFragment()

        toolbar = findViewById(R.id.toolbar)

        toolbar.title = when(fragment) {

            is ChargeDischargeFragment -> {

                getString(if(status == BatteryManager.BATTERY_STATUS_CHARGING)
                    R.string.charge else R.string.discharge)
            }

            is WearFragment -> getString(R.string.wear)
            is SettingsFragment -> getString(R.string.settings)
            is DebugFragment -> getString(R.string.debug)
            else -> getString(R.string.app_name)
        }

        toolbar.navigationIcon = null

        if(fragment !is SettingsFragment && fragment !is DebugFragment) inflateMenu()

        toolbar.setNavigationOnClickListener {

            onBackPressed()
        }

        navigation = findViewById(R.id.navigation)

        val chargeDischargeNavigation = navigation.menu.findItem(
            R.id.charge_discharge_navigation)

        chargeDischargeNavigation.title = getString(if(status ==
            BatteryManager.BATTERY_STATUS_CHARGING) R.string.charge else R.string.discharge)

        chargeDischargeNavigation.icon = getDrawable(if(status ==
            BatteryManager.BATTERY_STATUS_CHARGING) R.drawable.ic_charge_navigation_24dp
        else R.drawable.ic_discharge_navigation_24dp)

        navigation.menu.findItem(R.id.debug_navigation).isVisible =
            pref.getBoolean("debug_options_is_enabled", false)

        navigation.setOnNavigationItemSelectedListener {

            when(it.itemId) {

                R.id.charge_discharge_navigation -> {

                    if(fragment !is ChargeDischargeFragment) {

                        batteryIntent = registerReceiver(null, IntentFilter(
                            Intent.ACTION_BATTERY_CHANGED))

                        status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS,
                            BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

                        toolbar.title = getString(if(status == BatteryManager.BATTERY_STATUS_CHARGING)
                            R.string.charge else R.string.discharge)

                        toolbar.navigationIcon = null

                        clearMenu()

                        inflateMenu()

                        fragment = ChargeDischargeFragment()

                        loadFragment(fragment ?: ChargeDischargeFragment())
                    }
                }

                R.id.wear_navigation -> {

                    if(fragment !is WearFragment) {

                        toolbar.title = getString(R.string.wear)

                        toolbar.navigationIcon = null

                        clearMenu()

                        inflateMenu()

                        fragment = WearFragment()

                        loadFragment(fragment ?: ChargeDischargeFragment())
                    }
                }

                R.id.settings_navigation -> {

                    if(fragment !is SettingsFragment) {

                        toolbar.title = getString(R.string.settings)

                        toolbar.navigationIcon = null

                        clearMenu()

                        fragment = SettingsFragment()

                        loadFragment(fragment ?: ChargeDischargeFragment())
                    }
                }

                R.id.debug_navigation -> {

                    if(fragment !is DebugFragment) {

                        toolbar.title = getString(R.string.debug)

                        toolbar.navigationIcon = null

                        clearMenu()

                        fragment = DebugFragment()

                        loadFragment(fragment ?: ChargeDischargeFragment())
                    }
                }
            }

            true
        }

        loadFragment(fragment ?: ChargeDischargeFragment())
    }

    override fun onResume() {

        super.onResume()

        instance = this

        if(CapacityInfoService.instance == null && !isStartedService) {

            isStartedService = true

            onStartService(this, CapacityInfoService::class.java)
        }

        batteryIntent = registerReceiver(null, IntentFilter(
            Intent.ACTION_BATTERY_CHANGED))

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        toolbar.title = when(fragment) {

            is ChargeDischargeFragment -> {

                getString(if(status == BatteryManager.BATTERY_STATUS_CHARGING)
                    R.string.charge else R.string.discharge)
            }

            is WearFragment -> getString(R.string.wear)
            is SettingsFragment -> getString(R.string.settings)
            is DebugFragment -> getString(R.string.debug)
            else -> getString(R.string.app_name)
        }

        val chargeDischargeNavigation = navigation.menu.findItem(
            R.id.charge_discharge_navigation)

        chargeDischargeNavigation.title = getString(if(status ==
            BatteryManager.BATTERY_STATUS_CHARGING) R.string.charge else R.string.discharge)

        chargeDischargeNavigation.icon = getDrawable(if(status ==
            BatteryManager.BATTERY_STATUS_CHARGING) R.drawable.ic_charge_navigation_24dp
        else R.drawable.ic_discharge_navigation_24dp)

        navigation.menu.findItem(R.id.debug_navigation).isVisible =
            pref.getBoolean("debug_options_is_enabled", false)

        if(pref.getInt(DESIGN_CAPACITY, 0) <= 0
            || pref.getInt(DESIGN_CAPACITY, 0) > MAX_DESIGN_CAPACITY) {

            pref.edit().apply {

                putInt(DESIGN_CAPACITY, onGetDesignCapacity(this@MainActivity))

                if(pref.getInt(DESIGN_CAPACITY, 0) < 0)
                    putInt(DESIGN_CAPACITY, (pref.getInt(DESIGN_CAPACITY, 0) / -1))

                apply()
            }
        }

        if(onGetCurrentCapacity(this) == 0.0
            && pref.getBoolean(IS_SHOW_NOT_SUPPORTED_DIALOG, true)) {

            pref.edit().putBoolean(IS_SHOW_NOT_SUPPORTED_DIALOG, false).apply()

            pref.edit().putBoolean(IS_SUPPORTED, false).apply()

            MaterialAlertDialogBuilder(this).apply {

                setIcon(R.drawable.ic_instruction_not_supported_24dp)
                setTitle(getString(R.string.information))
                setMessage(getString(R.string.not_supported))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                show()
            }
        }

        if(onGetCurrentCapacity(this) == 0.0)
            toolbar.menu.findItem(R.id.instruction).isVisible = false

        else if(pref.getBoolean(IS_SHOW_INSTRUCTION, true)) showInstruction()

        val prefArrays = intent.getSerializableExtra(IMPORT_SETTINGS_EXTRA)
                as? HashMap<*, *>
        if(prefArrays != null) importSettings(prefArrays)

        if(OverlayService.instance == null && OverlayInterface.isEnabledOverlay(this))
            onStartService(this, OverlayService::class.java)
    }

    override fun onBackPressed() {

        if(toolbar.title != getString(R.string.settings) && fragment is SettingsFragment) {

            toolbar.title = getString(R.string.settings)

            toolbar.navigationIcon = null

            supportFragmentManager.beginTransaction().apply {

                replace(R.id.fragment_container, SettingsFragment())
                addToBackStack(null)
                commit()
            }
        }

        else finish()
    }

    override fun onStop() {

        super.onStop()

        instance = null
    }

    override fun onDestroy() {

        instance = null

        fragment = null

        super.onDestroy()
    }

    fun inflateMenu() {

        toolbar.inflateMenu(R.menu.main_menu)

        toolbar.menu.findItem(R.id.instruction).setOnMenuItemClickListener {

            showInstruction()

            true
        }

        toolbar.menu.findItem(R.id.faq).setOnMenuItemClickListener {

            MaterialAlertDialogBuilder(this).apply {

                setIcon(R.drawable.ic_faq_question_24dp)
                setTitle(getString(R.string.faq))
                setMessage(getString(R.string.faq_how_does_the_app_work)
                        + getString(R.string.faq_capacity_added)
                        + getString(R.string.faq_i_have_everything_in_zeros)
                        + getString(R.string.faq_units) + getString(R.string.faq_current_capacity)
                        + getString(R.string.faq_add_device_support))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                show()
            }

            true
        }

        toolbar.menu.findItem(R.id.tips).setOnMenuItemClickListener {

            MaterialAlertDialogBuilder(this).apply {

                setIcon(R.drawable.ic_tips_for_extending_battery_life_24dp)
                setTitle(getString(R.string.tips_dialog_title))
                setMessage(getString(R.string.tip1) + getString(R.string.tip2)
                        + getString(R.string.tip3) + getString(R.string.tip4)
                        + getString(R.string.tip5) + getString(R.string.tip6)
                        + getString(R.string.tip7))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                show()
            }

            true
        }
    }

    fun clearMenu() = toolbar.menu.clear()

    private fun showInstruction() {

        MaterialAlertDialogBuilder(this).apply {

            setIcon(R.drawable.ic_instruction_not_supported_24dp)
            setTitle(getString(R.string.instruction))
            setMessage(getString(R.string.instruction_message)
                    + getString(R.string.instruction_message_do_not_kill_the_service)
                    + getString(R.string.instruction_message_huawei_honor))

            setPositiveButton(android.R.string.ok) { _, _ ->

                if(pref.getBoolean(IS_SHOW_INSTRUCTION, true))
                    pref.edit().putBoolean(IS_SHOW_INSTRUCTION, false).apply()
            }

            show()
        }
    }

    fun loadFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction().apply {

            replace(R.id.fragment_container, fragment)
            addToBackStack(null)
            commit()
        }
    }

    private fun importSettings(prefArrays: HashMap<*, *>) {

        val prefsTempList = arrayListOf(BATTERY_LEVEL_TO, BATTERY_LEVEL_WITH,
            DESIGN_CAPACITY, CAPACITY_ADDED, LAST_CHARGE_TIME, PERCENT_ADDED, RESIDUAL_CAPACITY,
            IS_SUPPORTED, IS_SHOW_NOT_SUPPORTED_DIALOG, IS_SHOW_INSTRUCTION)

        prefsTempList.forEach {

            with(prefArrays) {

                when {

                    !containsKey(it) -> pref.edit().remove(it).apply()

                    else -> {

                        prefArrays.forEach {

                            when(it.key as String) {

                                NUMBER_OF_CHARGES -> pref.edit().putLong(it.key as String,
                                    it.value as Long).apply()

                                BATTERY_LEVEL_TO, BATTERY_LEVEL_WITH, LAST_CHARGE_TIME,
                                DESIGN_CAPACITY, RESIDUAL_CAPACITY, PERCENT_ADDED ->
                                    pref.edit().putInt(it.key as String, it.value as Int).apply()

                                CAPACITY_ADDED, NUMBER_OF_CYCLES ->
                                    pref.edit().putFloat(it.key as String,
                                        it.value as Float).apply()

                                IS_SUPPORTED, IS_SHOW_NOT_SUPPORTED_DIALOG, IS_SHOW_INSTRUCTION ->
                                    pref.edit().putBoolean(it.key as String,
                                        it.value as Boolean).apply()
                            }
                        }
                    }
                }
            }
        }

        toolbar.menu.clear()

        navigation.selectedItemId = R.id.settings_navigation

        Toast.makeText(this, getString(R.string.settings_imported_successfully),
            Toast.LENGTH_LONG).show()

        intent.removeExtra(IMPORT_SETTINGS_EXTRA)
    }
}