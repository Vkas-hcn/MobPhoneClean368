package com.carefree.and.joyous

import com.carefree.and.joyous.databinding.ItemFileBinding
import com.carefree.and.joyous.ui.FileSizeUtils



class FileScanAdapter(
    private val files: List<TrashFile>,
    private val onSelectionChanged: () -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<FileScanAdapter.FileViewHolder>() {

    class FileViewHolder(val binding: ItemFileBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        with(holder.binding) {
            tvFileName.text = file.getName()
            val (size, unit) = FileSizeUtils.formatFileSize(file.getSize())
            tvFileSize.text = "$size$unit"

            imgFileSelect.setImageResource(
                if (file.isSelected()) R.drawable.chooe else R.drawable.dischooe
            )

            root.setOnClickListener {
                toggleFileSelection(file, position)
            }

            imgFileSelect.setOnClickListener {
                toggleFileSelection(file, position)
            }
        }
    }

    override fun getItemCount() = files.size

    private fun toggleFileSelection(file: TrashFile, position: Int) {
        file.setSelected(!file.isSelected())
        notifyItemChanged(position)
        onSelectionChanged()
    }
}