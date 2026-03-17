package com.simpleFinApp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.simpleFinApp.adapters.SpendingAdapter
import com.simpleFinApp.data.PrefsManager
import com.simpleFinApp.databinding.ActivityMainBinding
import com.simpleFinApp.models.Spending

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: SpendingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PrefsManager(this)
        if (!prefs.isSetupDone) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) refreshUI()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SetupActivity::class.java))
                true
            }
            R.id.action_reset_month -> {
                showResetMonthDialog()
                true
            }
            R.id.action_manage_labels -> {
                showManageLabelsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = SpendingAdapter(
            prefs.getSpendings().toMutableList(),
            prefs.currencySymbol
        ) { spending ->
            showDeleteDialog(spending)
        }
        binding.rvSpendings.layoutManager = LinearLayoutManager(this)
        binding.rvSpendings.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showAddSpendingDialog() }
    }

    private fun refreshUI() {
        val symbol = prefs.currencySymbol
        val income = prefs.monthlyIncome
        val totalSpent = prefs.getTotalSpent()
        val remaining = prefs.getRemainingBudget()

        binding.tvCurrency.text = "${prefs.currency} ($symbol)"
        binding.tvIncome.text = "$symbol${String.format("%.2f", income)}"
        binding.tvTotalSpent.text = "$symbol${String.format("%.2f", totalSpent)}"
        binding.tvRemaining.text = "$symbol${String.format("%.2f", remaining)}"

        // Color remaining based on value
        val remainingColor = when {
            remaining < 0 -> getColor(R.color.red_spent)
            remaining < income * 0.2 -> getColor(R.color.orange_warning)
            else -> getColor(R.color.green_remaining)
        }
        binding.tvRemaining.setTextColor(remainingColor)
        binding.tvRemainingLabel.setTextColor(remainingColor)

        // Progress bar
        val progress = if (income > 0) ((totalSpent / income) * 100).toInt().coerceIn(0, 100) else 0
        binding.progressSpending.progress = progress

        val spendings = prefs.getSpendings()
        if (spendings.isEmpty()) {
            binding.rvSpendings.visibility = View.GONE
            binding.tvNoSpendings.visibility = View.VISIBLE
        } else {
            binding.rvSpendings.visibility = View.VISIBLE
            binding.tvNoSpendings.visibility = View.GONE
            adapter.updateData(spendings)
        }
    }

    private fun showAddSpendingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_spending, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val tilLabel = dialogView.findViewById<TextInputLayout>(R.id.tilLabel)
        val actvLabel = dialogView.findViewById<AutoCompleteTextView>(R.id.actvLabel)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)

        val labels = prefs.getLabels()
        val labelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
        actvLabel.setAdapter(labelAdapter)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_spending)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val amountStr = etAmount.text?.toString()?.trim()
                val label = actvLabel.text?.toString()?.trim()

                if (amountStr.isNullOrEmpty()) {
                    Toast.makeText(this, R.string.error_amount_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, R.string.error_amount_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (label.isNullOrEmpty()) {
                    Toast.makeText(this, R.string.error_label_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Auto-save new labels
                if (!labels.contains(label)) prefs.addLabel(label)

                val note = etNote.text?.toString()?.trim() ?: ""
                prefs.addSpending(Spending(amount = amount, label = label, note = note))
                refreshUI()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(spending: Spending) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_spending)
            .setMessage(getString(R.string.delete_spending_confirm, spending.label,
                "${prefs.currencySymbol}${String.format("%.2f", spending.amount)}"))
            .setPositiveButton(R.string.delete) { _, _ ->
                prefs.removeSpending(spending.id)
                refreshUI()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showResetMonthDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_month)
            .setMessage(R.string.reset_month_confirm)
            .setPositiveButton(R.string.reset) { _, _ ->
                prefs.saveSpendings(emptyList())
                refreshUI()
                Toast.makeText(this, R.string.month_reset_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showManageLabelsDialog() {
        val labels = prefs.getLabels().toMutableList()
        val items = labels.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.manage_labels)
            .setItems(items) { _, _ -> }
            .setPositiveButton(R.string.add_label) { _, _ ->
                showAddLabelDialog()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showAddLabelDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.label_hint)
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_label)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val label = input.text?.toString()?.trim()
                if (!label.isNullOrEmpty()) {
                    prefs.addLabel(label)
                    Toast.makeText(this, getString(R.string.label_added, label), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
