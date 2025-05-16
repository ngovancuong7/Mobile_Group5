package com.example.dictionary.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.data.model.ChatMessage
import com.example.dictionary.databinding.ItemChatMessageBinding

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: ChatMessage) {
            binding.tvMessage.text = chatMessage.message
            binding.cardMessage.setCardBackgroundColor(
                if (chatMessage.isUser) {
                    binding.root.context.getColor(android.R.color.holo_blue_light)
                } else {
                    binding.root.context.getColor(android.R.color.white)
                }
            )
            binding.tvMessage.setTextColor(
                if (chatMessage.isUser) {
                    binding.root.context.getColor(android.R.color.white)
                } else {
                    binding.root.context.getColor(android.R.color.black)
                }
            )
            binding.root.layoutDirection = if (chatMessage.isUser) {
                ViewGroup.LAYOUT_DIRECTION_RTL
            } else {
                ViewGroup.LAYOUT_DIRECTION_LTR
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
