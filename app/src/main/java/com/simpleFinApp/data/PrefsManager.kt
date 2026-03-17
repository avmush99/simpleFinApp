package com.simpleFinApp.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simpleFinApp.models.Spending

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CURRENCY = "currency"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_MONTHLY_INCOME = "monthly_income"
        private const val KEY_SPENDINGS = "spendings"
        private const val KEY_LABELS = "labels"
        private const val KEY_SETUP_DONE = "setup_done"
    }

    var isSetupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var currencySymbol: String
        get() = prefs.getString(KEY_CURRENCY_SYMBOL, "$") ?: "$"
        set(value) = prefs.edit().putString(KEY_CURRENCY_SYMBOL, value).apply()

    var monthlyIncome: Double
        get() = prefs.getFloat(KEY_MONTHLY_INCOME, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_MONTHLY_INCOME, value.toFloat()).apply()

    fun getSpendings(): MutableList<Spending> {
        val json = prefs.getString(KEY_SPENDINGS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Spending>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveSpendings(spendings: List<Spending>) {
        prefs.edit().putString(KEY_SPENDINGS, gson.toJson(spendings)).apply()
    }

    fun addSpending(spending: Spending) {
        val list = getSpendings()
        list.add(0, spending)
        saveSpendings(list)
    }

    fun removeSpending(id: String) {
        val list = getSpendings().filter { it.id != id }
        saveSpendings(list)
    }

    fun getLabels(): MutableList<String> {
        val json = prefs.getString(KEY_LABELS, null)
        if (json == null) {
            val defaults = mutableListOf("Food", "Transport", "Entertainment", "Health", "Shopping", "Bills", "Other")
            saveLabels(defaults)
            return defaults
        }
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveLabels(labels: List<String>) {
        prefs.edit().putString(KEY_LABELS, gson.toJson(labels)).apply()
    }

    fun addLabel(label: String) {
        val list = getLabels()
        if (!list.contains(label)) {
            list.add(label)
            saveLabels(list)
        }
    }

    fun getTotalSpent(): Double = getSpendings().sumOf { it.amount }

    fun getRemainingBudget(): Double = monthlyIncome - getTotalSpent()
}
