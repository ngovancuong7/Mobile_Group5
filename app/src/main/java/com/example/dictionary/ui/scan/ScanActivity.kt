package com.example.dictionary.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.dictionary.databinding.ActivityScanBinding
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.TextToSpeechManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private val viewModel: ScanViewModel by viewModels()

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager

    private var currentPhotoUri: Uri? = null

    // Biến để theo dõi trạng thái ngôn ngữ hiện tại
    private var isSourceVietnamese = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Cần quyền truy cập camera để chụp ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Cần quyền truy cập thư viện ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                processImage(uri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            processImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.btnCamera.setOnClickListener {
            checkCameraPermission()
        }

        binding.btnGallery.setOnClickListener {
            checkGalleryPermission()
        }

        binding.btnTranslate.setOnClickListener {
            val text = binding.etDetectedText.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                val sourceLanguage = if (isSourceVietnamese) "vi" else "en"
                val targetLanguage = if (isSourceVietnamese) "en" else "vi"
                viewModel.translateText(text, sourceLanguage, targetLanguage)
            }
        }

        // Xử lý sự kiện click trên nút swap
        binding.btnSwapLanguage.setOnClickListener {
            // Đảo ngược trạng thái ngôn ngữ
            isSourceVietnamese = !isSourceVietnamese

            // Cập nhật UI
            updateLanguageDisplay()

            // Nếu đã có văn bản và kết quả dịch, thực hiện hoán đổi
            val currentText = binding.etDetectedText.text.toString().trim()
            val currentTranslation = binding.tvTranslation.text.toString().trim()

            if (currentText.isNotEmpty() && currentTranslation.isNotEmpty() &&
                currentTranslation != "Translation result will appear here") {
                // Hoán đổi văn bản gốc và bản dịch
                binding.etDetectedText.setText(currentTranslation)
                binding.tvTranslation.text = currentText
            }
        }

        // Thêm nút phát âm cho văn bản được nhận dạng
        binding.btnSourceSpeaker.setOnClickListener {
            val text = binding.etDetectedText.text.toString().trim()
            if (text.isNotEmpty()) {
                val language = if (isSourceVietnamese) "vi" else "en"
                textToSpeechManager.speak(text, language)
            }
        }

        // Thêm nút phát âm cho bản dịch
        binding.btnTranslationSpeaker.setOnClickListener {
            val text = binding.tvTranslation.text.toString().trim()
            if (text.isNotEmpty() && text != "Translation result will appear here") {
                val language = if (isSourceVietnamese) "en" else "vi"
                textToSpeechManager.speak(text, language)
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    // Hàm cập nhật hiển thị ngôn ngữ
    private fun updateLanguageDisplay() {
        binding.tvSourceLanguage.text = if (isSourceVietnamese) "Tiếng Việt" else "English"
        binding.tvTargetLanguage.text = if (isSourceVietnamese) "English" else "Tiếng Việt"
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.recognizedText.collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.etDetectedText.setText(result.data)
                        binding.btnTranslate.isEnabled = true
                    }
                    is NetworkResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ScanActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnTranslate.isEnabled = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.translationResult.collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        binding.progressBarTranslation.visibility = View.GONE
                        binding.tvTranslation.text = result.data
                    }
                    is NetworkResult.Error -> {
                        binding.progressBarTranslation.visibility = View.GONE
                        Toast.makeText(this@ScanActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        binding.progressBarTranslation.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile.also {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            takePictureLauncher.launch(currentPhotoUri)
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun processImage(uri: Uri) {
        binding.ivPreview.setImageURI(uri)
        binding.ivPreview.visibility = View.VISIBLE
        viewModel.recognizeTextFromImage(uri, contentResolver)
    }
}
