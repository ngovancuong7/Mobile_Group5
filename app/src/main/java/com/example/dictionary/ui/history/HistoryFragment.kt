package com.example.dictionary.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dictionary.R
import com.example.dictionary.databinding.FragmentHistoryBinding
import com.example.dictionary.util.TextToSpeechManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
// Thêm import cho TextWatcher
import android.text.Editable
import android.text.TextWatcher
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class HistoryFragment : Fragment(), HistoryAdapter.HistoryListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMenuProvider()
        setupSearchView() // Thêm dòng này
        observeData()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(this)
        binding.recyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_history, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_all -> {
                        // Hiển thị hộp thoại xác nhận xóa tất cả
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.confirm_delete)
                            .setMessage(R.string.confirm_delete_all_history_message)
                            .setPositiveButton(R.string.delete_all) { _, _ ->
                                viewModel.deleteAllHistory()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWords.collectLatest { words ->
                historyAdapter.submitList(words)
                binding.tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onItemClick(word: String) {
        // Navigate to dictionary fragment with the word
        val action = R.id.action_historyFragment_to_dictionaryFragment
        val bundle = Bundle().apply {
            putString("word", word)
        }
        findNavController().navigate(action, bundle)
    }

    override fun onFavoriteClick(word: String) {
        viewModel.toggleFavorite(word)
    }

    override fun onSpeakerClick(word: String, language: String) {
        textToSpeechManager.speak(word, language)
    }

    override fun onDeleteClick(word: String) {
        // Hiển thị hộp thoại xác nhận trước khi xóa
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.confirm_delete_word, word))
            .setPositiveButton(R.string.delete_selected) { _, _ ->
                viewModel.deleteWord(word)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Trong phương thức setupUI() hoặc thêm phương thức mới setupSearchView()
    private fun setupSearchView() {
        binding.etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Thêm nút xóa text khi có nội dung
        binding.etSearchHistory.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (binding.etSearchHistory.compoundDrawables[2] != null) {
                    if (event.rawX >= (binding.etSearchHistory.right - binding.etSearchHistory.compoundDrawables[2].bounds.width())) {
                        binding.etSearchHistory.setText("")
                        return@setOnTouchListener true
                    }
                }
            }
            return@setOnTouchListener false
        }
    }
}
