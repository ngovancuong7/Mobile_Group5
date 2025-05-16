package com.example.dictionary.ui.favorite

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dictionary.R
import com.example.dictionary.databinding.FragmentFavoriteBinding
import com.example.dictionary.util.TextToSpeechManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FavoriteFragment : Fragment(), FavoriteAdapter.FavoriteListener {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var favoriteAdapter: FavoriteAdapter

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
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
        favoriteAdapter = FavoriteAdapter(this)
        binding.recyclerView.apply {
            adapter = favoriteAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    // Thêm phương thức setupSearchView
    private fun setupSearchView() {
        binding.etSearchFavorite.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Thêm nút xóa text khi có nội dung
        binding.etSearchFavorite.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (binding.etSearchFavorite.compoundDrawables[2] != null) {
                    if (event.rawX >= (binding.etSearchFavorite.right - binding.etSearchFavorite.compoundDrawables[2].bounds.width())) {
                        binding.etSearchFavorite.setText("")
                        return@setOnTouchListener true
                    }
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun setupMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_favorite, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val isInSelectionMode = viewModel.isInSelectionMode.value
                menu.findItem(R.id.action_select_all).isVisible = isInSelectionMode
                menu.findItem(R.id.action_remove_selected).isVisible = isInSelectionMode
                menu.findItem(R.id.action_select_mode).isVisible = !isInSelectionMode
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_select_mode -> {
                        viewModel.toggleSelectionMode()
                        true
                    }
                    R.id.action_select_all -> {
                        viewModel.selectAll()
                        true
                    }
                    R.id.action_remove_selected -> {
                        viewModel.removeSelectedFromFavorites()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordsWithSelection.collectLatest { words ->
                favoriteAdapter.submitList(words)
                binding.tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isInSelectionMode.collectLatest { isInSelectionMode ->
                favoriteAdapter.setSelectionMode(isInSelectionMode)
                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.selectedItemsCount.observe(viewLifecycleOwner) { count ->
            if (viewModel.isInSelectionMode.value) {
                requireActivity().title = getString(R.string.selected_items_count, count)
            } else {
                requireActivity().title = getString(R.string.favorites)
            }
        }
    }

    override fun onItemClick(word: String) {
        if (viewModel.isInSelectionMode.value) {
            viewModel.toggleSelection(word)
        } else {
            // Navigate to dictionary fragment with the word
            val action = R.id.action_favoriteFragment_to_dictionaryFragment
            val bundle = Bundle().apply {
                putString("word", word)
            }
            findNavController().navigate(action, bundle)
        }
    }

    override fun onItemLongClick(word: String): Boolean {
        if (!viewModel.isInSelectionMode.value) {
            viewModel.toggleSelectionMode()
        }
        viewModel.toggleSelection(word)
        return true
    }

    override fun onFavoriteClick(word: String) {
        viewModel.toggleFavorite(word)
    }

    override fun onSpeakerClick(word: String, language: String) {
        textToSpeechManager.speak(word, language)
    }

    override fun onNoteClick(word: String, currentNote: String?) {
        val editText = EditText(requireContext()).apply {
            setText(currentNote)
            hint = getString(R.string.note_hint)
            setSingleLine(false)
            minLines = 3
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (currentNote.isNullOrEmpty()) R.string.add_note else R.string.edit_note)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val note = editText.text.toString().trim()
                val finalNote = if (note.isEmpty()) null else note
                viewModel.updateNote(word, finalNote)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
