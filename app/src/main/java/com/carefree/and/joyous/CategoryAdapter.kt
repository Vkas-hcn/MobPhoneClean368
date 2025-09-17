package com.carefree.and.joyous

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.carefree.and.joyous.databinding.ItemCategoryBinding
import com.carefree.and.joyous.ui.FileSizeUtils


class CategoryAdapter(
    private val categories: List<TrashCategory>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        with(holder.binding) {
            ivIcon.setImageResource(category.getIconRes())
            tvTitle.text = category.getName()
            val (size, unit) = FileSizeUtils.formatFileSize(category.getTotalSize())
            tvSize.text = "$size$unit"

            imgInstruct.setImageResource(
                if (category.isExpanded()) R.drawable.ic_disex else R.drawable.ic_ex
            )

            updateCategorySelection(category)

            imgSelect.setImageResource(
                if (category.isSelected()) R.drawable.chooe else R.drawable.dischooe
            )

            if (category.isExpanded()) {
                rvItemFile.visibility = android.view.View.VISIBLE
                val fileAdapter = FileScanAdapter(category.getFiles()) {
                    updateCategorySelection(category)
                    notifyItemChanged(position)
                    onSelectionChanged()
                }
                rvItemFile.apply {
                    layoutManager = androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.context)
                    adapter = fileAdapter
                }
            } else {
                rvItemFile.visibility = android.view.View.GONE
            }

            llCategory.setOnClickListener {
                category.setExpanded(!category.isExpanded())
                notifyItemChanged(position)
            }

            imgSelect.setOnClickListener {
                category.setSelected(!category.isSelected())
                category.getFiles().forEach { it.setSelected(category.isSelected()) }
                notifyItemChanged(position)
                onSelectionChanged()
            }
        }
    }

    override fun getItemCount() = categories.size

    private fun updateCategorySelection(category: TrashCategory) {
        category.setSelected(category.getFiles().isNotEmpty() && category.getFiles().all { it.isSelected() })
    }

}