package br.com.uri.campushub.student

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.databinding.ItemEventPublicationBinding
import br.com.uri.campushub.model.EventPublication
import br.com.uri.campushub.model.PublicationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PublicationAdapter(
    private val currentUserId: String?,
    private val onDeletePublication: (EventPublication) -> Unit
) : ListAdapter<EventPublication, PublicationAdapter.PublicationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicationViewHolder {
        val binding = ItemEventPublicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PublicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PublicationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PublicationViewHolder(
        private val binding: ItemEventPublicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(publication: EventPublication) {
            binding.textPublicationAuthor.text = publication.authorName
            binding.textPublicationDate.text = formatDate(publication.createdAt)
            binding.buttonDeletePublication.visibility = if (publication.userId == currentUserId) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.buttonDeletePublication.setOnClickListener {
                onDeletePublication(publication)
            }

            when (publication.type) {
                PublicationType.PHOTO -> {
                    binding.imagePublication.visibility = View.VISIBLE
                    binding.textUnsupportedPublication.visibility = View.GONE
                    binding.imagePublication.setImageBitmap(
                        decodeSampledBitmap(publication.localPath, 1000, 700)
                    )
                }

                PublicationType.AUDIO -> showMediaLabel("Áudio gravado")
                PublicationType.VIDEO -> showMediaLabel("Vídeo publicado")
            }
        }

        private fun showMediaLabel(label: String) {
            binding.imagePublication.visibility = View.GONE
            binding.textUnsupportedPublication.visibility = View.VISIBLE
            binding.textUnsupportedPublication.text = label
        }
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat(
            "dd/MM/yyyy 'às' HH:mm",
            Locale.forLanguageTag("pt-BR")
        )
        return formatter.format(Date(timestamp))
    }

    private fun decodeSampledBitmap(path: String, requestedWidth: Int, requestedHeight: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)

        var sampleSize = 1
        while (
            boundsOptions.outWidth / sampleSize > requestedWidth * 2 ||
            boundsOptions.outHeight / sampleSize > requestedHeight * 2
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }

    private object DiffCallback : DiffUtil.ItemCallback<EventPublication>() {
        override fun areItemsTheSame(
            oldItem: EventPublication,
            newItem: EventPublication
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: EventPublication,
            newItem: EventPublication
        ): Boolean = oldItem == newItem
    }
}
