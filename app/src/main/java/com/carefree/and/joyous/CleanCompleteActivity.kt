package com.carefree.and.joyous


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.carefree.and.joyous.databinding.ActivityCleanCompleteBinding

class CleanCompleteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCleanCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCleanCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deletedSize = intent.getLongExtra("deleted_size", 0)
        startCountdown()
        setupViews(deletedSize)
        updateLogoResource()
    }

    private fun updateLogoResource() {
        val logoResource = when (MainActivity.jumpType) {
            0 -> R.drawable.logo_img     // 图片清理页面
            1 -> R.drawable.logo_file    // 清理文件页面
            else -> R.drawable.logo_clean // 清理垃圾页面
        }
        binding.imgLogo.setImageResource(logoResource)
    }

    private fun startCountdown() {
        binding.conClean.isVisible = true
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 1500
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            binding.pg.progress = progress
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.conClean.isVisible = false
            }
        })
        animator.start()
    }

    private fun setupViews(deletedSize: Long) {
        binding.tvSaveData.text = "Saved ${formatFileSize(deletedSize)} space for you"
        binding.tvBack.setOnClickListener {
            finish()
        }
        binding.imgBack.setOnClickListener {
            finish()
        }
        binding.conClean.setOnClickListener {

        }
        binding.llPic.setOnClickListener {
            startActivity(Intent(this, CleanPictureActivity::class.java))
            finish()
        }
        binding.llFile.setOnClickListener {
            startActivity(Intent(this, CleanFileActivity::class.java))
            finish()
        }
        binding.llClean.setOnClickListener {
            startActivity(Intent(this, CleanTrashActivity::class.java))
            finish()
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2fMB", size / (1024.0 * 1024.0))
            else -> String.format("%.2fKB", size / 1024.0)
        }
    }
}