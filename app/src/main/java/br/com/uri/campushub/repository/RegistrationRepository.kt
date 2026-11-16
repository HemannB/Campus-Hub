package br.com.uri.campushub.repository

import br.com.uri.campushub.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class RegistrationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RegisteredEventIdsDataSource, EnrollmentDataSource {
    override fun getRegisteredEventIds(onResult: (Result<Set<String>>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }
        firestore.collection("registrations").whereEqualTo("userId", userId).get()
            .addOnCompleteListener { task ->
                onResult(if (task.isSuccessful) Result.success(task.result.documents
                    .mapNotNull { it.getString("eventId") }.toSet())
                else Result.failure(task.exception ?: Exception("Não foi possível carregar suas inscrições.")))
            }
    }

    override fun getRegistration(eventId: String, onResult: (Result<Registration?>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }
        val document = registrationDocument(userId, eventId)
        firestore.runTransaction { transaction ->
            val eventDocument = firestore.collection("events").document(eventId)
            val event = transaction.get(eventDocument).toObject(Event::class.java)
                ?: error("Evento não encontrado.")
            val registration = transaction.get(document).toObject(Registration::class.java)
                ?.copy(id = document.id)
            val offer = if (event.eventStatus == EventStatus.PUBLISHED && !EventRatingEligibility.hasEnded(event)) {
                seats(event).offerNext()
            } else seats(event) to null
            val nextDocument = offer.second?.let { registrationDocument(it, eventId) }
            if (nextDocument != null) {
                transaction.update(nextDocument, "status", RegistrationStatus.INVITED.name)
                updateSeats(transaction, eventDocument, offer.first)
            }
            if (registration != null && registration.registrationStatus == RegistrationStatus.CONFIRMED &&
                registration.qrToken.isBlank()) {
                val token = UUID.randomUUID().toString()
                transaction.update(document, "qrToken", token)
                registration.copy(qrToken = token)
            } else if (offer.second == userId) registration?.copy(status = RegistrationStatus.INVITED.name)
            else registration
        }.addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) Result.success(task.result)
                else Result.failure(task.exception ?: Exception("Não foi possível consultar a inscrição.")))
        }
    }

    override fun register(eventId: String, onResult: (Result<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }
        val eventDocument = firestore.collection("events").document(eventId)
        val document = registrationDocument(userId, eventId)
        val profileDocument = firestore.collection("users").document(userId)
        firestore.runTransaction { transaction ->
            val event = transaction.get(eventDocument).toObject(Event::class.java)
                ?: error("Evento não encontrado.")
            val existing = transaction.get(document)
            val profile = transaction.get(profileDocument).toObject(User::class.java)
            checkNotNull(profile) { "Perfil não encontrado." }
            check(profile.userRole == UserRole.STUDENT) { "Somente alunos podem se inscrever." }
            check(event.eventStatus == EventStatus.PUBLISHED && !EventRatingEligibility.hasEnded(event)) {
                "Este evento não está aberto para inscrições."
            }
            check(!existing.exists()) { "Você já possui uma inscrição ou está na fila." }
            val (seats, status) = seats(event).join(userId)
            transaction.set(document, mapOf(
                "userId" to userId, "userName" to profile.name,
                "eventId" to eventId, "status" to status.name,
                "registeredAt" to FieldValue.serverTimestamp(),
                "qrToken" to if (status == RegistrationStatus.CONFIRMED) UUID.randomUUID().toString() else ""
            ))
            updateSeats(transaction, eventDocument, seats)
        }.addOnCompleteListener { complete(it.isSuccessful, it.exception, onResult) }
    }

    override fun confirmInvitation(eventId: String, onResult: (Result<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }
        val eventDocument = firestore.collection("events").document(eventId)
        val document = registrationDocument(userId, eventId)
        firestore.runTransaction { transaction ->
            val event = transaction.get(eventDocument).toObject(Event::class.java) ?: error("Evento não encontrado.")
            val registration = transaction.get(document).toObject(Registration::class.java) ?: error("Inscrição não encontrada.")
            check(event.eventStatus == EventStatus.PUBLISHED && !EventRatingEligibility.hasEnded(event)) {
                "Este evento não está aberto para inscrições."
            }
            check(registration.registrationStatus == RegistrationStatus.INVITED) { "Nenhuma vaga foi oferecida." }
            val seats = seats(event).confirm(userId)
            transaction.update(document, mapOf(
                "status" to RegistrationStatus.CONFIRMED.name,
                "qrToken" to UUID.randomUUID().toString()
            ))
            updateSeats(transaction, eventDocument, seats)
        }.addOnCompleteListener { complete(it.isSuccessful, it.exception, onResult) }
    }

    override fun cancel(eventId: String, onResult: (Result<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }
        val eventDocument = firestore.collection("events").document(eventId)
        val document = registrationDocument(userId, eventId)
        firestore.runTransaction { transaction ->
            val event = transaction.get(eventDocument).toObject(Event::class.java) ?: error("Evento não encontrado.")
            val registration = transaction.get(document).toObject(Registration::class.java) ?: error("Inscrição não encontrada.")
            check(registration.checkedInAt == null) { "Uma presença confirmada não pode ser cancelada." }
            val (seats, nextUserId) = seats(event).cancel(userId, registration.registrationStatus)
            val nextDocument = nextUserId?.let { registrationDocument(it, eventId) }
            transaction.delete(document)
            if (nextDocument != null) transaction.update(nextDocument, "status", RegistrationStatus.INVITED.name)
            updateSeats(transaction, eventDocument, seats)
        }.addOnCompleteListener { complete(it.isSuccessful, it.exception, onResult) }
    }

    private fun registrationDocument(userId: String, eventId: String) =
        firestore.collection("registrations").document("${userId}_$eventId")

    private fun seats(event: Event) = EventSeats(
        event.maxParticipants, event.participantCount, event.waitingUserIds, event.invitedUserIds
    )

    private fun updateSeats(
        transaction: com.google.firebase.firestore.Transaction,
        document: com.google.firebase.firestore.DocumentReference,
        seats: EventSeats
    ) {
        transaction.update(document, mapOf(
            "participantCount" to seats.confirmed,
            "waitingUserIds" to seats.waiting,
            "invitedUserIds" to seats.invited
        ))
    }

    private fun complete(success: Boolean, exception: Exception?, onResult: (Result<Unit>) -> Unit) {
        onResult(if (success) Result.success(Unit)
            else Result.failure(exception ?: Exception("Não foi possível atualizar a inscrição.")))
    }
}
