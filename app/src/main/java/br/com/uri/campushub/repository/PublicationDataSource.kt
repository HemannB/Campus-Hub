package br.com.uri.campushub.repository

import android.net.Uri
import br.com.uri.campushub.model.EventPublication
import br.com.uri.campushub.model.PublicationType

interface PublicationDataSource {
    fun getPublications(eventId: String): Result<List<EventPublication>>

    fun publishFromUri(
        eventId: String,
        authorName: String,
        type: PublicationType,
        sourceUri: Uri
    ): Result<EventPublication>

    fun deletePublication(publicationId: String): Result<Unit>
}
