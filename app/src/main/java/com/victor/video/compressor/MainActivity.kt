package com.victor.video.compressor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.victor.video.compressor.databinding.ActivityMainBinding
import com.victor.video.compressor.library.CompressionResult
import com.victor.video.compressor.library.VideoCompressor
import com.victor.video.compressor.library.settings.CompressionSettings
import com.victor.video.compressor.library.settings.EncoderSelectionMode
import com.victor.video.compressor.library.utils.CompressUtil
import com.victor.video.compressor.library.utils.CompressUtil.formatLength
import com.victor.video.compressor.library.utils.CompressUtil.getBitrate
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var start = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        initView()
    }

    fun initView() {
        binding.mBtnSelectVideo.setOnClickListener {
            pickVideoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        }
        binding.mBtnViewCompressedVideo.setOnClickListener {
            val out = binding.mTvCompressedFileName.text.toString()
            viewFile(File(out))
        }
    }

    fun compressAction(uri: Uri) {
        lifecycleScope.launch {
            val input = CompressUtil.copyFileToLocal(this@MainActivity,uri) ?: return@launch
            val output = File(filesDir, "${input.nameWithoutExtension}_compressed.mp4")
            start = System.currentTimeMillis()

            VideoCompressor.compress(
                context = this@MainActivity,
                input = input,
                output = output,
                onMetadataDecoded = { compressor, metadata ->
                    binding.mTvOriginalBitrate.text = CompressUtil.formatBitrate(metadata.bitrate)
                    val maxDimension = max(metadata.actualHeight, metadata.actualWidth)
                    val ratio = 720f / maxDimension

                    CompressionSettings.Builder()
                        .setTargetSize(
                            width = (metadata.actualWidth * ratio).toInt(),
                            height = (metadata.actualHeight * ratio).toInt()
                        )
                        .setBitrate(max(2_000_000, metadata.bitrate / 3))
                        .setFastStart(true)
                        .allowSizeAdjustments(true)
                        .setEncoderSelectionMode(EncoderSelectionMode.TRY_ALL)
                        .build()
                }
            ).collect { result ->
                showCompressStatus(result)
            }
        }
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        // 用户选择完成后的回调
        if (uri != null) {
            // 成功获取到视频的 Uri
            // 注意：这个 Uri 是只读的，可以直接用于播放或上传
            Log.d("VideoPicker", "选中的视频 Uri: $uri")
            // 例如：videoView.setVideoURI(uri) 或 上传文件
            compressAction(uri)
        } else {
            // 用户取消了选择
            Log.d("VideoPicker", "用户未选择视频")
        }
    }

    private fun viewFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setData(uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        runCatching { startActivity(intent) }
    }

    fun showCompressStatus(result: CompressionResult) {
        when (result) {
            is CompressionResult.Started -> {
                binding.mTvStatus.text = "Started to compress video ${result.inputFile.name}"
                binding.mTvOriginalFileName.text = result.inputFile.name
                binding.mTvOriginalSize.text = result.inputFile.formatLength()
                binding.mPbCompressProgress.progress = 0
                val output = File(filesDir, "${result.inputFile.nameWithoutExtension}_compressed.mp4")
                start = System.currentTimeMillis()

                binding.mBtnSelectVideo.visibility = View.GONE
                binding.mBtnViewCompressedVideo.visibility = View.GONE
                binding.mTvCompressed.visibility = View.GONE
                binding.mTvCompressedFileName.text = output.absolutePath
                binding.mTvCompressedSize.text = ""
                binding.mTvCompressedBitrate.text = ""
            }

            is CompressionResult.Progress -> {
                binding.mTvStatus.text = "Compressing"
                binding.mPbCompressProgress.progress = (result.progress * 100).toInt()
            }

            CompressionResult.Cancelled -> {
                binding.mBtnSelectVideo.visibility = View.VISIBLE
                binding.mTvStatus.text = "Cancelled"
            }

            is CompressionResult.Error -> {
                binding.mBtnSelectVideo.visibility = View.VISIBLE
                binding.mTvStatus.text = result.error.message
            }

            is CompressionResult.Success -> {
                binding.mBtnViewCompressedVideo.visibility = View.VISIBLE
                binding.mTvCompressed.visibility = View.VISIBLE
                binding.mBtnSelectVideo.visibility = View.VISIBLE
                binding.mTvCompressedSize.text = result.outFile.formatLength()
                val end = System.currentTimeMillis()
                val duration = end - start
                binding.mTvStatus.text = "Success duration: ${CompressUtil.formatDuration(duration)}"
                binding.mTvCompressedBitrate.text = CompressUtil.formatBitrate(result.outFile.getBitrate())
            }
        }
    }



}