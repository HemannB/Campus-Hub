package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Rating
import br.com.uri.campushub.model.RatingSummary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RatingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getRatingSummary(
        eventId: String,
        onResult: (Result<RatingSummary>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        firestore.collection("ratings")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar as avaliações.")
                    onResult(Result.failure(exception))
                    return@addOnCompleteListener
                }

                val ratings = task.result
                    ?.documents
                    ?.mapNotNull { document ->
                        document.toObject(Rating::class.java)
                            ?.copy(id = document.id)
                    }
                    ?.filter { rating -> rating.score in 1..5 }
                    .orEmpty()

                val summary = RatingSummary(
                    average = ratings
                        .map { rating -> rating.score }
                        .average()
                        .takeUnless { average -> average.isNaN() }
                        ?: 0.0,
                    count = ratings.size,
                    currentUserScore = ratings
                        .firstOrNull { rating -> rating.userId == userId }
                        ?.score
                )

                onResult(Result.success(summary))
            }
    }

    fun saveRating(
        eventId: String,
        score: Int,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (score !in 1..5) {
            onResult(Result.failure(Exception("A nota deve estar entre 1 e 5.")))
            return
        }

        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        val eventDocument = firestore.collection("events").document(eventId)
        val registrationDocument = firestore.collection("registrations")
            .document("${userId}_$eventId")
        val ratingDocument = firestore.collection("ratings")
            .document("${userId}_$eventId")

        firestore.runTransaction { transaction ->
            val eventSnapshot = transaction.get(eventDocument)
            val registrationSnapshot = transaction.get(registrationDocument)
            val ratingSnapshot = transaction.get(ratingDocument)

            if (!eventSnapshot.exists()) {
                throw IllegalStateException("Evento não encontrado.")
            }

            if (!registrationSnapshot.exists()) {
                throw IllegalStateException(
                    "Somente alunos inscritos podem avaliar este evento."
                )
            }

            val eventStart = eventSnapshot.getTimestamp("startAt")
            val eventEnd = eventSnapshot.getTimestamp("endAt")

            if (eventStart == null || eventEnd == null || eventEnd.compareTo(eventStart) < 0) {
                throw IllegalStateException(
                    "O evento possui datas inválidas e não pode ser avaliado."
                )
            }

            if (eventEnd.compareTo(Timestamp.now()) > 0) {
                throw IllegalStateException(
                    "A avaliação estará disponível quando o evento terminar."
                )
            }

            if (ratingSnapshot.exists()) {
                transaction.update(
                    ratingDocument,
                    mapOf(
                        "score" to score,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                transaction.set(
                    ratingDocument,
                    mapOf(
                        "eventId" to eventId,
                        "userId" to userId,
                        "score" to score,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(Result.success(Unit))
            } else {
                val businessError = task.exception?.cause
                    ?.takeIf { cause -> cause is IllegalStateException }
                val exception = businessError
                    ?: task.exception
                    ?: Exception("Não foi possível salvar a avaliação.")

                onResult(Result.failure(exception))
            }
        }
    }
}
