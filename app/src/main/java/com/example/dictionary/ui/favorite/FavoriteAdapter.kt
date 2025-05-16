package com.example.dictionary.ui.favorite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.R
import com.example.dictionary.databinding.ItemWordFavoriteBinding
import com.example.dictionary.ui.favorite.FavoriteViewModel.WordWithSelection

class FavoriteAdapter(
    private val listener: FavoriteListener
) : ListAdapter<WordWithSelection, FavoriteAdapter.WordViewHolder>(WordDiffCallback()) {

    private var isSelectionMode = false

    fun setSelectionMode(isSelectionMode: Boolean) {
        this.isSelectionMode = isSelectionMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WordViewHolder(binding, listener, isSelectionMode)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position), isSelectionMode)
    }

    class WordViewHolder(
        private val binding: ItemWordFavoriteBinding,
        private val listener: FavoriteListener,
        private var isSelectionMode: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(wordWithSelection: WordWithSelection, isSelectionMode: Boolean) {
            this.isSelectionMode = isSelectionMode
            val wordWithFavorite = wordWithSelection.word
            val word = wordWithFavorite.word

            binding.tvWord.text = word.word
            binding.tvTranslation.text = word.translation
            binding.tvPhonetic.text = word.phonetic
            binding.tvPhonetic.visibility = if (word.phonetic.isNotEmpty()) View.VISIBLE else View.GONE

            // Set note if available
            if (wordWithFavorite.note.isNullOrEmpty()) {
                binding.tvNote.visibility = View.GONE
            } else {
                binding.tvNote.text = wordWithFavorite.note
                binding.tvNote.visibility = View.VISIBLE
            }

            // Set favorite button
            binding.btnFavorite.isSelected = true // Always true in the favorite fragment
            binding.btnFavorite.contentDescription = binding.root.context.getString(R.string.remove_from_favorites)

            // Set background color for selection state
            val backgroundColor = if (wordWithSelection.isSelected && isSelectionMode) {
                ContextCompat.getColor(binding.root.context, R.color.selection_color)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            }
            binding.cardContainer.setCardBackgroundColor(backgroundColor)

            // Setup click listeners
            binding.root.setOnClickListener {
                listener.onItemClick(word.word)
            }

            binding.root.setOnLongClickListener {
                listener.onItemLongClick(word.word)
            }

            binding.btnFavorite.setOnClickListener {
                listener.onFavoriteClick(word.word)
            }

            binding.btnSpeaker.setOnClickListener {
                listener.onSpeakerClick(word.word, "en")
            }

            binding.btnNote.setOnClickListener {
                listener.onNoteClick(word.word, wordWithFavorite.note)
            }
        }
    }

    class WordDiffCallback : DiffUtil.ItemCallback<WordWithSelection>() {
        override fun areItemsTheSame(oldItem: WordWithSelection, newItem: WordWithSelection): Boolean {
            return oldItem.word.word.word == newItem.word.word.word
        }

        override fun areContentsTheSame(oldItem: WordWithSelection, newItem: WordWithSelection): Boolean {
            return oldItem == newItem
        }
    }

    interface FavoriteListener {
        fun onItemClick(word: String)
        fun onItemLongClick(word: String): Boolean
        fun onFavoriteClick(word: String)
        fun onSpeakerClick(word: String, language: String)
        fun onNoteClick(word: String, currentNote: String?)
    }
}
