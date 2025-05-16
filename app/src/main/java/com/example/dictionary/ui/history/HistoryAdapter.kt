package com.example.dictionary.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.R
import com.example.dictionary.databinding.ItemWordHistoryBinding
import com.example.dictionary.data.model.Word

class HistoryAdapter(
    private val listener: HistoryListener
) : ListAdapter<Word, HistoryAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WordViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WordViewHolder(
        private val binding: ItemWordHistoryBinding,
        private val listener: HistoryListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(word: Word) {
            binding.tvWord.text = word.word
            binding.tvTranslation.text = word.translation
            binding.tvPhonetic.text = word.phonetic
            binding.tvPhonetic.visibility = if (word.phonetic.isNotEmpty()) ViewGroup.VISIBLE else ViewGroup.GONE

            // Cập nhật trạng thái yêu thích
            binding.btnFavorite.isSelected = word.isFavorite
            // Thay đổi màu sắc của icon trái tim dựa trên trạng thái yêu thích
            binding.btnFavorite.setColorFilter(
                ContextCompat.getColor(
                    binding.root.context,
                    if (word.isFavorite) R.color.red else R.color.black
                )
            )

            // Setup click listeners
            binding.root.setOnClickListener {
                listener.onItemClick(word.word)
            }

            binding.btnFavorite.setOnClickListener {
                listener.onFavoriteClick(word.word)
            }

            binding.btnSpeaker.setOnClickListener {
                listener.onSpeakerClick(word.word, "en")
            }

            binding.btnDelete.setOnClickListener {
                listener.onDeleteClick(word.word)
            }
        }
    }

    class WordDiffCallback : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem.word == newItem.word
        }

        override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem == newItem
        }
    }

    interface HistoryListener {
        fun onItemClick(word: String)
        fun onFavoriteClick(word: String)
        fun onSpeakerClick(word: String, language: String)
        fun onDeleteClick(word: String)
    }
}
