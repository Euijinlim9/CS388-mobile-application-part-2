package com.example.cs388_mobile_application_part_2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val notificationsSwitch = view.findViewById<SwitchMaterial>(R.id.switch_notifications)
        val calendarSwitch = view.findViewById<SwitchMaterial>(R.id.switch_google_calendar)
        val statusText = view.findViewById<TextView>(R.id.settings_status)
        val versionText = view.findViewById<TextView>(R.id.settings_app_version)

        versionText.text = "Version 1.0"

        val updateStatus = {
            val notificationsState = if (notificationsSwitch.isChecked) {
                "On"
            } else {
                "Off"
            }

            val calendarState = if (calendarSwitch.isChecked) {
                "On"
            } else {
                "Off"
            }

            statusText.text = "Notifications: $notificationsState | Calendar sync: $calendarState"
        }

        notificationsSwitch.setOnCheckedChangeListener { _, _ -> updateStatus() }
        calendarSwitch.setOnCheckedChangeListener { _, _ -> updateStatus() }
        updateStatus()

        return view
    }
}