package com.example.dictionary.ui.dictionary

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dictionary.R
import com.example.dictionary.data.model.DictionaryEntry
import com.example.dictionary.databinding.FragmentDictionaryBinding
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.TextToSpeechManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DictionaryFragment : Fragment() {

    private var _binding: FragmentDictionaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DictionaryViewModel by viewModels()

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager

    private var currentWord = ""
    private var mediaPlayer: MediaPlayer? = null
    private var audioUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDictionaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeData()

        // Kiểm tra và xử lý tham số từ navigation
        arguments?.getString("word")?.let { word ->
            if (word.isNotEmpty()) {
                currentWord = word
                binding.etWord.setText(word)
                viewModel.lookupWord(word)
            }
        }

        // Khôi phục trạng thái từ ViewModel nếu có
        viewModel.lastLookedUpWord.observe(viewLifecycleOwner) { lastWordData ->
            if (lastWordData != null && lastWordData.word.isNotEmpty() && currentWord.isEmpty()) {
                currentWord = lastWordData.word
                binding.etWord.setText(lastWordData.word)
                binding.tvWord.text = lastWordData.word

                // Không cần gọi API lại vì ViewModel đã có dữ liệu
                if (binding.resultContainer.visibility != View.VISIBLE) {
                    viewModel.lookupWord(lastWordData.word)
                }
            }
        }
    }

    private fun setupUI() {
        binding.etWord.doAfterTextChanged { text ->
            binding.btnSearch.isEnabled = !text.isNullOrBlank()
        }

        binding.btnSearch.setOnClickListener {
            val word = binding.etWord.text.toString().trim()
            if (word.isNotEmpty()) {
                currentWord = word
                viewModel.lookupWord(word)
                hideKeyboard()
            }
        }

        binding.btnSourceSpeaker.setOnClickListener {
            if (audioUrl != null && audioUrl!!.isNotEmpty()) {
                playAudio(audioUrl!!)
            } else if (currentWord.isNotEmpty()) {
                textToSpeechManager.speak(currentWord, "en")
            }
        }

        binding.btnTranslationSpeaker.setOnClickListener {
            val translation = binding.tvTranslation.text.toString()
            if (translation.isNotEmpty()) {
                textToSpeechManager.speak(translation, "vi")
            }
        }

        binding.btnFavorite.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                viewModel.toggleFavorite(currentWord)
            }
        }

        binding.btnShare.setOnClickListener {
            shareWord()
        }
    }

    private fun observeData() {
        viewModel.lookupResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.resultContainer.visibility = View.VISIBLE

                    val (translation, entries) = result.data!!
                    displayWordDetails(currentWord, translation, entries)
                }
                is NetworkResult.Error -> {
                    binding.progressBar.visibility = View.GONE

                    if (result.data != null) {
                        // Hiển thị bản dịch nếu có, ngay cả khi không tìm thấy thông tin chi tiết
                        val (translation, _) = result.data
                        binding.resultContainer.visibility = View.VISIBLE
                        displayBasicTranslation(currentWord, translation)
                    } else {
                        binding.resultContainer.visibility = View.GONE
                    }

                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.resultContainer.visibility = View.GONE
                }
            }
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFavorite ->
            updateFavoriteButton(isFavorite)
        }
    }

    private fun displayBasicTranslation(word: String, translation: String) {
        // Đảm bảo từ tiếng Anh luôn được hiển thị
        binding.tvWord.text = word
        binding.etWord.setText(word)
        currentWord = word

        binding.tvTranslation.text = translation

        // Ẩn các thông tin chi tiết khác
        binding.tvPhonetic.visibility = View.GONE
        binding.tvPartOfSpeech.visibility = View.GONE
        binding.tvSynonyms.visibility = View.GONE
        binding.tvAntonyms.visibility = View.GONE
        binding.tvExample1.visibility = View.GONE
        binding.tvExample2.visibility = View.GONE

        // Reset audio URL
        audioUrl = null
    }

    private fun displayWordDetails(word: String, translation: String, entries: List<DictionaryEntry>) {
        // Đảm bảo từ tiếng Anh luôn được hiển thị
        binding.tvWord.text = word
        binding.etWord.setText(word)
        currentWord = word

        binding.tvTranslation.text = translation

        if (entries.isNotEmpty()) {
            val entry = entries[0]

            // Hiển thị phát âm
            val phonetic = entry.phonetic ?: entry.phonetics?.firstOrNull()?.text ?: ""
            binding.tvPhonetic.text = phonetic
            binding.tvPhonetic.visibility = if (phonetic.isNotEmpty()) View.VISIBLE else View.GONE

            // Lưu URL audio để phát âm
            audioUrl = findBestAudioUrl(entry.phonetics)

            // Hiển thị từ loại và định nghĩa
            val meanings = entry.meanings ?: emptyList()
            if (meanings.isNotEmpty()) {
                val meaning = meanings[0]
                binding.tvPartOfSpeech.text = meaning.partOfSpeech ?: ""
                binding.tvPartOfSpeech.visibility = View.VISIBLE

                // Hiển thị ví dụ
                val examples = mutableListOf<String>()
                meaning.definitions?.forEachIndexed { index, definition ->
                    if (definition.example != null && definition.example.isNotEmpty()) {
                        if (index < 2) { // Chỉ hiển thị tối đa 2 ví dụ
                            examples.add(definition.example)
                        }
                    }
                }

                if (examples.isNotEmpty()) {
                    binding.tvExample1.text = getString(R.string.example_format, 1, examples[0])
                    binding.tvExample1.visibility = View.VISIBLE

                    if (examples.size > 1) {
                        binding.tvExample2.text = getString(R.string.example_format, 2, examples[1])
                        binding.tvExample2.visibility = View.VISIBLE
                    } else {
                        binding.tvExample2.visibility = View.GONE
                    }
                } else {
                    binding.tvExample1.visibility = View.GONE
                    binding.tvExample2.visibility = View.GONE
                }

                // Hiển thị từ đồng nghĩa và trái nghĩa
                val synonyms = meaning.synonyms ?: emptyList()
                val antonyms = meaning.antonyms ?: emptyList()

                if (synonyms.isNotEmpty()) {
                    binding.tvSynonyms.text = getString(R.string.synonyms_format, synonyms.take(3).joinToString(", "))
                    binding.tvSynonyms.visibility = View.VISIBLE
                } else {
                    binding.tvSynonyms.visibility = View.GONE
                }

                if (antonyms.isNotEmpty()) {
                    binding.tvAntonyms.text = getString(R.string.antonyms_format, antonyms.take(3).joinToString(", "))
                    binding.tvAntonyms.visibility = View.VISIBLE
                } else {
                    binding.tvAntonyms.visibility = View.GONE
                }
            } else {
                binding.tvPartOfSpeech.visibility = View.GONE
                binding.tvExample1.visibility = View.GONE
                binding.tvExample2.visibility = View.GONE
                binding.tvSynonyms.visibility = View.GONE
                binding.tvAntonyms.visibility = View.GONE
            }
        } else {
            // Ẩn các thông tin chi tiết nếu không có dữ liệu
            binding.tvPhonetic.visibility = View.GONE
            binding.tvPartOfSpeech.visibility = View.GONE
            binding.tvSynonyms.visibility = View.GONE
            binding.tvAntonyms.visibility = View.GONE
            binding.tvExample1.visibility = View.GONE
            binding.tvExample2.visibility = View.GONE

            // Reset audio URL
            audioUrl = null
        }
    }

    private fun findBestAudioUrl(phonetics: List<DictionaryEntry.Phonetic>?): String? {
        if (phonetics == null || phonetics.isEmpty()) return null

        // Ưu tiên phát âm có audio
        for (phonetic in phonetics) {
            if (!phonetic.audio.isNullOrEmpty()) {
                return phonetic.audio
            }
        }

        return null
    }

    private fun playAudio(url: String) {
        try {
            releaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { releaseMediaPlayer() }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(requireContext(), "Không thể phát âm thanh", Toast.LENGTH_SHORT).show()
                    releaseMediaPlayer()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi phát âm: ${e.message}", Toast.LENGTH_SHORT).show()
            // Fallback to TTS if media player fails
            textToSpeechManager.speak(currentWord, "en")
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        binding.btnFavorite.text = if (isFavorite) {
            getString(R.string.remove_from_favorites)
        } else {
            getString(R.string.add_to_favorites)
        }

        binding.btnFavorite.setBackgroundResource(
            if (isFavorite) R.drawable.bg_rounded_button_red else R.drawable.bg_rounded_button
        )
    }

    private fun shareWord() {
        if (currentWord.isEmpty()) return

        val translation = binding.tvTranslation.text.toString()
        val phonetic = binding.tvPhonetic.text.toString()
        val partOfSpeech = binding.tvPartOfSpeech.text.toString()

        val shareText = buildString {
            append("$currentWord")
            if (phonetic.isNotEmpty()) append(" $phonetic")
            append("\n")
            append("Nghĩa: $translation")
            if (partOfSpeech.isNotEmpty()) append("\nTừ loại: $partOfSpeech")

            if (binding.tvSynonyms.visibility == View.VISIBLE) {
                append("\n${binding.tvSynonyms.text}")
            }

            if (binding.tvAntonyms.visibility == View.VISIBLE) {
                append("\n${binding.tvAntonyms.text}")
            }

            if (binding.tvExample1.visibility == View.VISIBLE) {
                append("\n${binding.tvExample1.text}")
            }

            if (binding.tvExample2.visibility == View.VISIBLE) {
                append("\n${binding.tvExample2.text}")
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ từ vựng")
        startActivity(shareIntent)
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etWord.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseMediaPlayer()
        _binding = null
    }
}
