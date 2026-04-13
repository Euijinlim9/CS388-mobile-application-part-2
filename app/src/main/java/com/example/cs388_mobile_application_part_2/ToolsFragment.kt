package com.example.cs388_mobile_application_part_2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class ToolsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTipCalculator(view)
        setupUnitConverter(view)
        setupPasswordGenerator(view)
    }

    private fun setupTipCalculator(view: View) {
        val etBill = view.findViewById<EditText>(R.id.etBillAmount)
        val etTip = view.findViewById<EditText>(R.id.etTipPercent)
        val tvResult = view.findViewById<TextView>(R.id.tvTipResult)

        view.findViewById<Button>(R.id.btnCalcTip).setOnClickListener {
            val bill = etBill.text.toString().toDoubleOrNull()
            val tip = etTip.text.toString().toDoubleOrNull()
            if (bill != null && tip != null) {
                val tipAmount = bill * tip / 100
                val total = bill + tipAmount
                tvResult.text = "Tip: $%.2f | Total: $%.2f".format(tipAmount, total)
            } else {
                tvResult.text = "Please enter valid numbers"
            }
        }
    }

    private fun setupUnitConverter(view: View) {
        val etInput = view.findViewById<EditText>(R.id.etUnitInput)
        val tvResult = view.findViewById<TextView>(R.id.tvUnitResult)

        view.findViewById<Button>(R.id.btnKmToMiles).setOnClickListener {
            val value = etInput.text.toString().toDoubleOrNull()
            if (value != null) tvResult.text = "%.4f miles".format(value * 0.621371)
            else tvResult.text = "Enter a valid number"
        }

        view.findViewById<Button>(R.id.btnMilesToKm).setOnClickListener {
            val value = etInput.text.toString().toDoubleOrNull()
            if (value != null) tvResult.text = "%.4f km".format(value * 1.60934)
            else tvResult.text = "Enter a valid number"
        }
    }

    private fun setupPasswordGenerator(view: View) {
        val etLength = view.findViewById<EditText>(R.id.etPasswordLength)
        val tvPassword = view.findViewById<TextView>(R.id.tvPassword)
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()"

        view.findViewById<Button>(R.id.btnGenPassword).setOnClickListener {
            val length = etLength.text.toString().toIntOrNull()?.coerceIn(4, 64) ?: 16
            tvPassword.text = (1..length).map { chars.random() }.joinToString("")
        }
    }
}
