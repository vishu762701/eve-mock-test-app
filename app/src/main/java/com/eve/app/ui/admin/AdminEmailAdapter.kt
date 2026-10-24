package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.databinding.ItemAdminEmailBinding

class AdminEmailAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<AdminEmailAdapter.VH>() {

    private var items: List<String> = emptyList()

    fun submit(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAdminEmailBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(email: String) {
            b.tvEmail.text = email
            b.btnRemove.setOnClickListener { onRemove(email) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminEmailBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
