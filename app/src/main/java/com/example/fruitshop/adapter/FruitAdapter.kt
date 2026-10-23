package com.example.fruitshop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fruitshop.R
import com.example.fruitshop.model.Fruit
import com.example.fruitshop.databinding.ItemFruitBinding
import com.example.fruitshop.utils.ValidationUtils
import java.util.Locale

class FruitAdapter(private val onClick: (Fruit) -> Unit) : RecyclerView.Adapter<FruitAdapter.Holder>() {
    private var items: List<Fruit> = emptyList()
    fun submit(items: List<Fruit>) { this.items = items; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemFruitBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val fruit = items[position]
        holder.binding.nameText.text = fruit.name
        holder.binding.infoText.text = "${fruit.category.ifBlank { "Fruit" }} • ${String.format(Locale.getDefault(), "%.2f", fruit.price)} • Stock ${fruit.quantity}\nExpires ${fruit.expiryDate}"
        val status = ValidationUtils.status(fruit)
        holder.binding.statusText.text = status
        holder.binding.statusText.setTextColor(ContextCompat.getColor(holder.itemView.context, if (status == "Expired") R.color.danger else if (status == "Near Expiry" || status == "Out of Stock") R.color.warning else R.color.green))
        holder.itemView.setOnClickListener { onClick(fruit) }
    }
    class Holder(val binding: ItemFruitBinding) : RecyclerView.ViewHolder(binding.root)
}

