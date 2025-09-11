package com.carefree.and.joyous


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.carefree.and.joyous.databinding.ItemFileCleanBinding
import com.carefree.and.joyous.ui.FileSizeUtils
import java.io.File

class FileAdapter(
    private val files: MutableList<FileItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder( var binding: ItemFileCleanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: FileItem, position: Int) {
            binding.tvFileName.text = file.name
            binding.tvFileSize.text = formatFileSize(file.size)
            val selectedImg = if(file.isSelected){R.drawable.chooe}else{R.drawable.dischooe}
            binding.ivSelectStatus.setImageResource(selectedImg)

            binding.root.setOnClickListener {
                onItemClick(position)
            }

            binding.ivSelectStatus.setOnClickListener {
                onItemClick(position)
            }

            when (file.type) {
                FileType.IMAGE -> {
                    loadImageThumbnail(file.path)
                }
                FileType.VIDEO -> {
                    loadVideoThumbnail(file.path)
                }
                else -> {
                    Glide.with(itemView.context).clear(binding.ivFileIcon)
                    binding.ivFileIcon.setImageResource(R.drawable.wj_file)
                }
            }
        }

        private fun loadImageThumbnail(imagePath: String) {
            val requestOptions = RequestOptions()
                .override(200, 200)
                .centerCrop() // 居中裁剪
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.kong)
                .error(R.drawable.kong)

            Glide.with(itemView.context)
                .load(File(imagePath))
                .apply(requestOptions)
                .into(binding.ivFileIcon)
        }

        private fun loadVideoThumbnail(videoPath: String) {
            val requestOptions = RequestOptions()
                .override(200, 200)
                .centerCrop() // 居中裁剪
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.kong)
                .error(R.drawable.kong)

            Glide.with(itemView.context)
                .load(File(videoPath))
                .apply(requestOptions)
                .into(binding.ivFileIcon)
        }

        private fun formatFileSize(size: Long): String {
            val (sizeValue, sizeUnit) = FileSizeUtils.formatFileSize(size)
            return "$sizeValue$sizeUnit"
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileCleanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position], position)
    }

    override fun getItemCount(): Int = files.size

    override fun onViewRecycled(holder: FileViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context).clear(holder.binding.ivFileIcon)
    }
}