package com.example.dictionary.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dictionary.BuildConfig
import com.example.dictionary.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // Hiển thị API key hiện tại
        binding.etApiKey.setText(viewModel.getCurrentApiKey())

        // Hiển thị API key mặc định từ BuildConfig
        binding.tvDefaultApiKey.text = "API key mặc định: ${BuildConfig.RAPID_API_KEY}"

        // Xử lý sự kiện khi nhấn nút lưu
        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            viewModel.saveApiKey(apiKey)
            Toast.makeText(requireContext(), "API key đã được lưu", Toast.LENGTH_SHORT).show()
        }

        // Xử lý sự kiện khi nhấn nút khôi phục
        binding.btnReset.setOnClickListener {
            binding.etApiKey.setText(BuildConfig.RAPID_API_KEY)
            viewModel.saveApiKey(BuildConfig.RAPID_API_KEY)
            Toast.makeText(requireContext(), "Đã khôi phục API key mặc định", Toast.LENGTH_SHORT).show()
        }
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeData() {
        // Có thể thêm các observer nếu cần
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
