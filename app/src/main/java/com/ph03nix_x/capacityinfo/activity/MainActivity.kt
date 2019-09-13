package com.ph03nix_x.capacityinfo.activity

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.net.Uri
import android.os.AsyncTask
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.ph03nix_x.capacityinfo.Battery
import com.ph03nix_x.capacityinfo.R
import com.ph03nix_x.capacityinfo.Preferences
import com.ph03nix_x.capacityinfo.async.DoAsync
import com.ph03nix_x.capacityinfo.services.*

@SuppressWarnings("StaticFieldLeak", "PrivateApi")
class MainActivity : AppCompatActivity() {

    private lateinit var capacityDesign: TextView
    private lateinit var batteryLevel: TextView
    private lateinit var residualCapacity: TextView
    private lateinit var currentCapacity: TextView
    private lateinit var technology: TextView
    private lateinit var status: TextView
    private lateinit var plugged: TextView
    private lateinit var chargingCurrent: TextView
    private lateinit var temperatute: TextView
    private lateinit var voltage: TextView
    private lateinit var lastChargeTime: TextView
    private lateinit var batteryWear: TextView
    private lateinit var pref: SharedPreferences
    private lateinit var relativeMain: RelativeLayout
    private lateinit var batteryManager: BatteryManager
    private lateinit var jobScheduler: JobScheduler
    private lateinit var job: JobInfo.Builder
    private var batteryStatus: Intent? = null
    private var isDoAsync = false

    companion object {

        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        pref = getSharedPreferences("preferences", Context.MODE_PRIVATE)

        if(pref.getBoolean(Preferences.DarkMode.prefName, false)) setTheme(R.style.DarkTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        relativeMain = findViewById(R.id.relative_main)
        capacityDesign = findViewById(R.id.capacity_design)
        batteryLevel = findViewById(R.id.battery_level)
        currentCapacity = findViewById(R.id.current_capacity)
        residualCapacity = findViewById(R.id.residual_capacity)
        technology = findViewById(R.id.battery_technology)
        status = findViewById(R.id.status)
        plugged = findViewById(R.id.plugged)
        chargingCurrent = findViewById(R.id.charging_current)
        temperatute = findViewById(R.id.temperature)
        voltage = findViewById(R.id.voltage)
        lastChargeTime = findViewById(R.id.last_charge_time)
        batteryWear = findViewById(R.id.battery_wear)

        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        if(pref.getBoolean(Preferences.DarkMode.prefName, false)) relativeMain.setBackgroundColor(ContextCompat.getColor(this, R.color.dark))

        if(pref.getBoolean(Preferences.IsShowInstruction.prefName, true)) {

            AlertDialog.Builder(this).apply {

                setIcon(if(pref.getBoolean(Preferences.DarkMode.prefName, false)) getDrawable(R.drawable.ic_info_white_24dp) else getDrawable(R.drawable.ic_info_black_24dp))
                setTitle(getString(R.string.instruction))
                setMessage(getString(R.string.instruction_message))
                setPositiveButton(android.R.string.ok) { _, _ -> pref.edit().putBoolean(Preferences.IsShowInstruction.prefName, false).apply() }
                show()
            }
        }

        if(pref.getBoolean(Preferences.EnableService.prefName, true)) startCapacityInfoJob()
    }

    override fun onResume() {

        super.onResume()

        val battery = Battery(this)

        var isShowDialog = true

        instance = this

        if(pref.getInt(Preferences.DesignCapacity.prefName, 0) <= 0 || pref.getInt(
                Preferences.DesignCapacity.prefName, 0) >= 100000) {

            pref.edit().putInt(Preferences.DesignCapacity.prefName, battery.getDesignCapacity()).apply()

            if(pref.getInt(Preferences.DesignCapacity.prefName, 0) < 0)
                pref.edit().putInt(
                    Preferences.DesignCapacity.prefName, (pref.getInt(
                        Preferences.DesignCapacity.prefName, 0) / -1)).apply()
        }

        capacityDesign.text = getString(R.string.capacity_design, pref.getInt(Preferences.DesignCapacity.prefName, 0).toString())

        residualCapacity.text = getString(R.string.residual_capacity, "0", "0%")

        batteryWear.text = getString(R.string.battery_wear, "0%")

        isDoAsync = true

        DoAsync {

            while(isDoAsync) {

                batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

                runOnUiThread {

                    batteryLevel.text = getString(R.string.battery_level, "${battery.getBatteryLevel()}%")
                }

                if(pref.getBoolean(Preferences.ShowLastChargeTime.prefName, true)) {

                    runOnUiThread {

                        if(lastChargeTime.visibility == View.GONE) lastChargeTime.visibility = View.VISIBLE

                        if(pref.getInt(Preferences.LastChargeTime.prefName, 0) > 0)
                            lastChargeTime.text = getString(R.string.last_charge_time, battery.getLastChargeTime(),
                                "${pref.getInt(Preferences.BatteryLevelWith.prefName, 0)}%", "${pref.getInt(Preferences.BatteryLevelTo.prefName, 0)}%")

                        else {

                            if(lastChargeTime.visibility == View.VISIBLE) lastChargeTime.visibility = View.GONE
                        }

                    }
                }

                else {

                    runOnUiThread {

                        if(lastChargeTime.visibility == View.VISIBLE) lastChargeTime.visibility = View.GONE

                    }
                }

                runOnUiThread {

                    this.status.text = battery.getStatus(status!!)

                    if(battery.getPlugged(plugged!!) != "N/A") {

                        if(this.plugged.visibility == View.GONE) this.plugged.visibility = View.VISIBLE

                        this.plugged.text = battery.getPlugged(plugged)
                    }

                    else this.plugged.visibility = View.GONE
                }

                runOnUiThread {

                    technology.text = getString(R.string.battery_technology, batteryStatus!!.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY))

                    temperatute.text = if (!pref.getBoolean(Preferences.Fahrenheit.prefName, false)) getString(R.string.temperature_celsius,
                        battery.getTemperature())

                    else getString(R.string.temperature_fahrenheit, battery.getTemperature())

                    voltage.text = getString(R.string.voltage, battery.toDecimalFormat(battery.getVoltage()))
                }

                if (pref.getBoolean(Preferences.IsSupported.prefName, true)) {

                        if (pref.getInt(Preferences.DesignCapacity.prefName, 0) > 0 && pref.getInt(
                                Preferences.ChargeCounter.prefName, 0) > 0) {

                            runOnUiThread {

                                residualCapacity.text = battery.getResidualCapacity()

                                batteryWear.text = battery.getBatteryWear()
                            }
                        }

                        if (battery.getCurrentCapacity() > 0) {

                            if (currentCapacity.visibility == View.GONE) runOnUiThread { currentCapacity.visibility = View.VISIBLE }

                            runOnUiThread {

                                currentCapacity.text = getString(R.string.current_capacity, battery.toDecimalFormat(battery.getCurrentCapacity()))
                            }

                        }

                        else {

                            if (currentCapacity.visibility == View.VISIBLE) runOnUiThread { currentCapacity.visibility = View.GONE }
                        }

                        val intentFilter = IntentFilter()

                        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)

                        batteryStatus = registerReceiver(null, intentFilter)

                        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {

                            if (chargingCurrent.visibility == View.GONE) runOnUiThread { chargingCurrent.visibility = View.VISIBLE }

                            runOnUiThread {

                                chargingCurrent.text = getString(R.string.charging_current, battery.getChargingCurrent().toString())
                            }

                        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING || status == BatteryManager.BATTERY_STATUS_FULL || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {

                            if (chargingCurrent.visibility == View.GONE) runOnUiThread { chargingCurrent.visibility = View.VISIBLE }

                            runOnUiThread {

                                chargingCurrent.text = getString(R.string.discharge_current, battery.getChargingCurrent().toString())
                            }
                        } else {

                            if (chargingCurrent.visibility == View.VISIBLE) runOnUiThread {  chargingCurrent.visibility = View.GONE }
                        }

                }

                else {

                    if(isShowDialog) {

                        isShowDialog = false

                        val dialog = AlertDialog.Builder(this).apply {

                            setIcon(if(pref.getBoolean(Preferences.DarkMode.prefName, false)) getDrawable(R.drawable.ic_info_white_24dp)
                            else getDrawable(R.drawable.ic_info_black_24dp))
                            setTitle(getString(R.string.information))
                            setMessage(getString(R.string.not_supported))
                            setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                        }

                        runOnUiThread {

                            dialog.show()
                        }
                    }
                }

                Thread.sleep(5000)
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun onStop() {

        super.onStop()

        isDoAsync = false
    }

    override fun onBackPressed() {

        super.onBackPressed()

        instance = null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.main_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {

            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))

            R.id.instruction -> AlertDialog.Builder(this).apply {

                setIcon(if(pref.getBoolean(Preferences.DarkMode.prefName, false)) getDrawable(R.drawable.ic_info_white_24dp) else getDrawable(R.drawable.ic_info_black_24dp))
                setTitle(getString(R.string.instruction))
                setMessage(getString(R.string.instruction_message))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                show()
            }

            R.id.github -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ph03niX-X/CapacityInfo")))
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startCapacityInfoJob() {

        val componentName = ComponentName(this, CapacityInfoJob::class.java)

        jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        job = JobInfo.Builder(1, componentName).apply {

            setMinimumLatency(1000)
            setRequiresCharging(true)
            setPersisted(false)
        }

        jobScheduler.schedule(job.build())
    }
}