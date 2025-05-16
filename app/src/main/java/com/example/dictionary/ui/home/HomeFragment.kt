package com.example.dictionary.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.dictionary.R
import com.example.dictionary.databinding.FragmentHomeBinding
import com.example.dictionary.ui.scan.ScanActivity
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.TextToSpeechManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager

    // Biến để theo dõi trạng thái ngôn ngữ hiện tại
    private var isSourceVietnamese = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupUI()
        observeData()
    }
    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.etText.windowToken, 0)
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupUI() {
        binding.btnTranslate.setOnClickListener {
            val text = binding.etText.text.toString().trim()
            if (text.isNotEmpty()) {
                hideKeyboard()
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
            val currentText = binding.etText.text.toString().trim()
            val currentTranslation = binding.tvTranslation.text.toString().trim()

            if (currentText.isNotEmpty() && currentTranslation.isNotEmpty() &&
                currentTranslation != "Translation result will appear here") {
                // Hoán đổi văn bản gốc và bản dịch
                binding.etText.setText(currentTranslation)
                binding.tvTranslation.text = currentText
            }
        }

        binding.btnFavorite.setOnClickListener {
            val word = binding.etText.text.toString().trim()
            if (word.isNotEmpty() && !word.contains(" ")) {
                viewModel.toggleFavorite(word)
            } else {
                Toast.makeText(requireContext(), "Chỉ có thể lưu từ đơn vào mục yêu thích", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnScan.setOnClickListener {
            try {
                val intent = Intent(requireContext(), ScanActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // Xử lý sự kiện click trên nút phát âm văn bản gốc
        binding.btnSpeaker.setOnClickListener {
            val text = binding.etText.text.toString().trim()
            if (text.isNotEmpty()) {
                val language = if (isSourceVietnamese) "vi" else "en"
                textToSpeechManager.speak(text, language)
            }
        }

        // Xử lý sự kiện click trên nút phát âm bản dịch
        binding.btnTranslationSpeaker.setOnClickListener {
            val text = binding.tvTranslation.text.toString().trim()
            if (text.isNotEmpty() && text != "Translation result will appear here") {
                val language = if (isSourceVietnamese) "en" else "vi"
                textToSpeechManager.speak(text, language)
            }
        }
    }

    // Hàm cập nhật hiển thị ngôn ngữ
    private fun updateLanguageDisplay() {
        binding.tvSourceLanguage.text = if (isSourceVietnamese) "Tiếng Việt" else "English"
        binding.tvTargetLanguage.text = if (isSourceVietnamese) "English" else "Tiếng Việt"
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.translationResult.collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvTranslation.text = result.data
                    }
                    is NetworkResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    null -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.currentWord.observe(viewLifecycleOwner) { word ->
            binding.btnFavorite.isSelected = word?.isFavorite ?: false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
