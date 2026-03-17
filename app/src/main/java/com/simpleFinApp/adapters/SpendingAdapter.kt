package com.simpleFinApp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simpleFinApp.R
import com.simpleFinApp.models.Spending
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpendingAdapter(
    private val spendings: MutableList<Spending>,
    private val symbol: String,
    private val onDelete: (Spending) -> Unit
) : RecyclerView.Adapter<SpendingAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvNote: TextView = view.findViewById(R.id.tvNote)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val spending = spendings[position]
        holder.tvLabel.text = spending.label
        holder.tvAmount.text = "-$symbol${String.format("%.2f", spending.amount)}"
        holder.tvDate.text = dateFormat.format(Date(spending.timestamp))
        if (spending.note.isNotEmpty()) {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = spending.note
        } else {
            holder.tvNote.visibility = View.GONE
        }
        holder.btnDelete.setOnClickListener { onDelete(spending) }
    }

    override fun getItemCount() = spendings.size

    fun updateData(newList: List<Spending>) {
        spendings.clear()
        spendings.addAll(newList)
        notifyDataSetChanged()
    }
}
