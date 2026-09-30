package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AdminTransactionDto
import com.eve.app.databinding.ItemAdminTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminTransactionAdapter(
    private var items: List<AdminTransactionDto> = emptyList()
) : RecyclerView.Adapter<AdminTransactionAdapter.TransactionViewHolder>() {

    fun submitList(newItems: List<AdminTransactionDto>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemAdminTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TransactionViewHolder(
        private val binding: ItemAdminTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        fun bind(item: AdminTransactionDto) {
            val context = binding.root.context
            binding.tvOrderAmount.text = "₹${item.amount}"
            binding.tvOrderMethod.text = "(${item.paymentMethod.uppercase()})"
            binding.tvOrderId.text = "ID: ${item.orderId}"
            binding.tvUserEmail.text = item.userEmail.ifBlank { item.userId }
            binding.tvOrderDate.text = "Created: ${dateFormat.format(Date(item.createdAt))}"

            val (statusText, colorRes) = when (item.status.uppercase()) {
                "SUCCESS" -> "SUCCESS" to R.color.eve_status_success
                "CREATED", "PENDING" -> item.status.uppercase() to R.color.eve_status_warning
                "EXPIRED" -> "EXPIRED" to R.color.eve_status_error
                "REFUNDED" -> "REFUNDED" to R.color.eve_status_info
                else -> item.status.uppercase() to R.color.eve_text_secondary
            }

            binding.tvOrderStatus.text = statusText
            binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, colorRes))
        }
    }
}
