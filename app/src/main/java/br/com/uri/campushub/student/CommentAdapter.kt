package br.com.uri.campushub.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.databinding.ItemCommentBinding
import br.com.uri.campushub.model.Comment
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter(
    private val currentUserId: String?,
    private val onEditComment: (Comment) -> Unit,
    private val onDeleteComment: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(
            comment = getItem(position),
            currentUserId = currentUserId,
            onEditComment = onEditComment,
            onDeleteComment = onDeleteComment
        )
    }

    class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            comment: Comment,
            currentUserId: String?,
            onEditComment: (Comment) -> Unit,
            onDeleteComment: (Comment) -> Unit
        ) {
            binding.textCommentAuthor.text = comment.authorName.ifBlank { "Aluno" }
            binding.textCommentDate.text = formatDate(comment.createdAt, comment.updatedAt)
            binding.textCommentBody.text = comment.text

            val isOwner = comment.userId == currentUserId
            binding.layoutCommentActions.visibility = if (isOwner) View.VISIBLE else View.GONE
            binding.buttonEditComment.setOnClickListener { onEditComment(comment) }
            binding.buttonDeleteComment.setOnClickListener { onDeleteComment(comment) }
        }

        private fun formatDate(createdAt: Timestamp?, updatedAt: Timestamp?): String {
            val date = createdAt?.toDate() ?: return "Agora"
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy 'às' HH:mm",
                Locale.forLanguageTag("pt-BR")
            )
            val suffix = if (updatedAt != null) " • editado" else ""
            return formatter.format(date) + suffix
        }
    }
}

private object CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem == newItem
    }
}
