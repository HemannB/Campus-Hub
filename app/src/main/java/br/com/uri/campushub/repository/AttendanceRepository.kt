package br.com.uri.campushub.repository

import br.com.uri.campushub.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AttendanceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AttendanceDataSource {
    override fun getAttendees(eventId: String, onResult: (Result<List<Registration>>) -> Unit) {
        firestore.collection("registrations").whereEqualTo("eventId", eventId).get()
            .addOnCompleteListener { task ->
                onResult(if (task.isSuccessful) Result.success(task.result.documents.mapNotNull { document ->
                    document.toObject(Registration::class.java)?.copy(id = document.id)
                }.sortedBy { it.registeredAt })
                else Result.failure(task.exception ?: Exception("Não foi possível carregar os inscritos.")))
            }
    }

    override fun checkIn(eventId: String, ticket: String, onResult: (Result<Unit>) -> Unit) {
        val parsed = CheckInTicket.parse(ticket)
        if (parsed == null || parsed.eventId != eventId) {
            onResult(Result.failure(Exception("QR Code inválido ou de outro evento.")))
            return
        }
        val document = firestore.collection("registrations").document(parsed.registrationId)
        firestore.runTransaction { transaction ->
            val event = transaction.get(firestore.collection("events").document(eventId))
                .toObject(Event::class.java) ?: error("Evento não encontrado.")
            val registration = transaction.get(document).toObject(Registration::class.java)
                ?: error("Inscrição não encontrada.")
            check(event.organizerId == auth.currentUser?.uid) { "Você não é responsável por este evento." }
            check(event.eventStatus == EventStatus.PUBLISHED && !EventRatingEligibility.hasEnded(event)) {
                "O evento não está aberto para check-in."
            }
            check(registration.eventId == eventId && registration.qrToken == parsed.token) { "QR Code inválido." }
            check(registration.registrationStatus == RegistrationStatus.CONFIRMED) { "A inscrição não está confirmada." }
            check(registration.checkedInAt == null) { "Esta inscrição já fez check-in." }
            transaction.update(document, "checkedInAt", FieldValue.serverTimestamp())
        }.addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) Result.success(Unit)
                else Result.failure(task.exception ?: Exception("Não foi possível registrar presença.")))
        }
    }
}
