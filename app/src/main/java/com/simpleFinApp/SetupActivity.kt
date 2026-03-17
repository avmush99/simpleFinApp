package com.simpleFinApp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simpleFinApp.data.CurrencyData
import com.simpleFinApp.data.PrefsManager
import com.simpleFinApp.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefs: PrefsManager
    private val currencies = CurrencyData.currencies
    private var selectedCurrencyIndex = currencies.indexOfFirst { it.code == "USD" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setupCurrencyDropdown()
        setupSaveButton()

        // Pre-fill if editing
        if (prefs.isSetupDone) {
            binding.etIncome.setText(prefs.monthlyIncome.toString())
            val idx = currencies.indexOfFirst { it.code == prefs.currency }
            if (idx >= 0) {
                selectedCurrencyIndex = idx
                (binding.actvCurrency as AutoCompleteTextView).setText(currencies[idx].toString(), false)
            }
            binding.btnSave.text = getString(R.string.update_settings)
        }
    }

    private fun setupCurrencyDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            currencies.map { it.toString() }
        )
        val actvCurrency = binding.actvCurrency as AutoCompleteTextView
        actvCurrency.setAdapter(adapter)
        actvCurrency.setText(currencies[selectedCurrencyIndex].toString(), false)

        actvCurrency.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val idx = currencies.indexOfFirst { it.toString() == text }
                if (idx >= 0) selectedCurrencyIndex = idx
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        actvCurrency.setOnItemClickListener { _, _, position, _ ->
            selectedCurrencyIndex = position
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val incomeStr = binding.etIncome.text?.toString()?.trim()
            if (incomeStr.isNullOrEmpty()) {
                binding.tilIncome.error = getString(R.string.error_income_required)
                return@setOnClickListener
            }
            val income = incomeStr.toDoubleOrNull()
            if (income == null || income < 0) {
                binding.tilIncome.error = getString(R.string.error_income_invalid)
                return@setOnClickListener
            }
            binding.tilIncome.error = null

            val currency = currencies[selectedCurrencyIndex]
            prefs.currency = currency.code
            prefs.currencySymbol = currency.symbol
            prefs.monthlyIncome = income
            prefs.isSetupDone = true

            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
    }
}
