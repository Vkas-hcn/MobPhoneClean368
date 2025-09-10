package com.carefree.and.joyous

import com.carefree.and.joyous.databinding.ItemCategoryBinding


class CategoryAdapter(
    private val categories: List<TrashCategory>,
    private val onSelectionChanged: () -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        with(holder.binding) {
            ivIcon.setImageResource(category.iconRes)
            tvTitle.text = category.name
            tvSize.text = formatFileSize(category.totalSize)

            imgInstruct.setImageResource(
                if (category.isExpanded) R.drawable.ic_disex else R.drawable.ic_ex
            )

            updateCategorySelection(category)

            imgSelect.setImageResource(
                if (category.isSelected) R.drawable.chooe else R.drawable.dischooe
            )

            if (category.isExpanded) {
                rvItemFile.visibility = android.view.View.VISIBLE
                val fileAdapter = FileScanAdapter(category.files) {
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
                category.isExpanded = !category.isExpanded
                notifyItemChanged(position)
            }

            imgSelect.setOnClickListener {
                category.isSelected = !category.isSelected
                category.files.forEach { it.isSelected = category.isSelected }
                notifyItemChanged(position)
                onSelectionChanged()
            }
        }
    }

    override fun getItemCount() = categories.size

    private fun updateCategorySelection(category: TrashCategory) {
        category.isSelected = category.files.isNotEmpty() && category.files.all { it.isSelected }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2fMB", size / (1024.0 * 1024.0))
            else -> String.format("%.2fKB", size / 1024.0)
        }
    }
}