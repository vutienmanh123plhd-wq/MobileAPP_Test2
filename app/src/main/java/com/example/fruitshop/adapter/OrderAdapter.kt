package com.example.fruitshop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fruitshop.model.Order
import com.example.fruitshop.databinding.ItemOrderBinding
import java.util.Locale

class OrderAdapter(private val onClick: (Order) -> Unit) : RecyclerView.Adapter<OrderAdapter.Holder>() {
    private var items: List<Order> = emptyList()
    fun submit(items: List<Order>) { this.items = items; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val order = items[position]
        holder.binding.orderTitle.text = "Order #${order.id}"
        holder.binding.orderSubtitle.text = "${order.orderDate} • Total ${String.format(Locale.getDefault(), "%.2f", order.totalAmount)}"
        holder.binding.orderStatus.text = order.status
        holder.itemView.setOnClickListener { onClick(order) }
    }
    class Holder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)
}

