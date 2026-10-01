package br.com.uri.campushub.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getRegisteredEventIds(
        onResult: (Result<Set<String>>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        firestore.collection("registrations")
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val eventIds = task.result
                        ?.documents
                        ?.mapNotNull { document ->
                            document.getString("eventId")
                        }
                        ?.toSet()
                        .orEmpty()

                    onResult(Result.success(eventIds))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar suas inscrições.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun getRegistrationStatus(
        eventId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        registrationDocumentId(userId, eventId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(task.result?.exists() == true))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível consultar a inscrição.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun register(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        val eventDocument = firestore.collection("events").document(eventId)
        val registrationDocument = registrationDocumentId(userId, eventId)

        firestore.runTransaction { transaction ->
            val eventSnapshot = transaction.get(eventDocument)
            val registrationSnapshot = transaction.get(registrationDocument)

            if (!eventSnapshot.exists()) {
                throw Exception("Evento não encontrado.")
            }

            if (registrationSnapshot.exists()) {
                throw Exception("Você já está inscrito neste evento.")
            }

            val participantCount = eventSnapshot.getLong("participantCount") ?: 0L
            val maxParticipants = eventSnapshot.getLong("maxParticipants") ?: 0L

            if (maxParticipants > 0 && participantCount >= maxParticipants) {
                throw Exception("Não há vagas disponíveis neste evento.")
            }

            val registration = hashMapOf(
                "userId" to userId,
                "eventId" to eventId,
                "registeredAt" to FieldValue.serverTimestamp()
            )

            transaction.set(registrationDocument, registration)
            transaction.update(eventDocument, "participantCount", participantCount + 1)
        }.addOnCompleteListener { task ->
            completeOperation(
                isSuccessful = task.isSuccessful,
                exception = task.exception,
                defaultMessage = "Não foi possível realizar a inscrição.",
                onResult = onResult
            )
        }
    }

    fun cancel(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        val eventDocument = firestore.collection("events").document(eventId)
        val registrationDocument = registrationDocumentId(userId, eventId)

        firestore.runTransaction { transaction ->
            val eventSnapshot = transaction.get(eventDocument)
            val registrationSnapshot = transaction.get(registrationDocument)

            if (!eventSnapshot.exists()) {
                throw Exception("Evento não encontrado.")
            }

            if (!registrationSnapshot.exists()) {
                throw Exception("Inscrição não encontrada.")
            }

            val participantCount = eventSnapshot.getLong("participantCount") ?: 0L

            transaction.delete(registrationDocument)
            transaction.update(
                eventDocument,
                "participantCount",
                (participantCount - 1).coerceAtLeast(0)
            )
        }.addOnCompleteListener { task ->
            completeOperation(
                isSuccessful = task.isSuccessful,
                exception = task.exception,
                defaultMessage = "Não foi possível cancelar a inscrição.",
                onResult = onResult
            )
        }
    }

    private fun registrationDocumentId(
        userId: String,
        eventId: String
    ) = firestore.collection("registrations").document("${userId}_$eventId")

    private fun completeOperation(
        isSuccessful: Boolean,
        exception: Exception?,
        defaultMessage: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (isSuccessful) {
            onResult(Result.success(Unit))
        } else {
            val message = exception?.cause?.message
                ?: exception?.message
                ?: defaultMessage
            onResult(Result.failure(Exception(message)))
        }
    }
}
