package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventStatus
import br.com.uri.campushub.model.eventStatus
import br.com.uri.campushub.model.EventValidator
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue

class EventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : EventDataSource, OrganizerEventDataSource, EventEditorDataSource {
    override fun updateEvent(event: Event, onResult: (Result<Unit>) -> Unit) {
        val document = firestore.collection("events").document(event.id)
        firestore.runTransaction { transaction ->
            val current = transaction.get(document).toObject(Event::class.java)
                ?: error("Evento não encontrado.")
            check(current.organizerId == auth.currentUser?.uid) { "Este evento não pertence a você." }
            check(current.eventStatus != EventStatus.ENDED) { "Eventos encerrados não podem ser editados." }
            check(event.maxParticipants >= current.participantCount + current.invitedUserIds.size) {
                "A capacidade não pode ser menor que as vagas ocupadas ou reservadas."
            }
            EventValidator.validate(event)?.let { error(it) }
            transaction.update(document, mapOf(
                "title" to event.title, "description" to event.description,
                "category" to event.category, "location" to event.location,
                "startAt" to event.startAt, "endAt" to event.endAt,
                "maxParticipants" to event.maxParticipants, "imageUrl" to event.imageUrl,
                "status" to current.eventStatus.name
            ))
        }.addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) Result.success(Unit)
                else Result.failure(task.exception ?: Exception("Não foi possível editar o evento.")))
        }
    }

    override fun changeStatus(eventId: String, status: EventStatus, onResult: (Result<Unit>) -> Unit) {
        val document = firestore.collection("events").document(eventId)
        firestore.runTransaction { transaction ->
            val current = transaction.get(document).toObject(Event::class.java)
                ?: error("Evento não encontrado.")
            check(current.organizerId == auth.currentUser?.uid) { "Este evento não pertence a você." }
            val now = Timestamp.now()
            when (status) {
                EventStatus.PUBLISHED -> {
                    check(current.eventStatus == EventStatus.DRAFT) { "Somente rascunhos podem ser publicados." }
                    EventValidator.validate(current)?.let { error(it) }
                    check(current.endAt!! > now) { "O evento já terminou. Ajuste as datas." }
                    transaction.update(document, "status", status.name)
                }
                EventStatus.ENDED -> {
                    check(current.eventStatus == EventStatus.PUBLISHED) { "Publique o evento antes de encerrá-lo." }
                    check(current.startAt != null && current.startAt <= now) { "O evento ainda não iniciou." }
                    transaction.update(document, mapOf("status" to status.name, "endAt" to FieldValue.serverTimestamp()))
                }
                EventStatus.DRAFT -> error("Não é possível voltar para rascunho.")
            }
        }.addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) Result.success(Unit)
                else Result.failure(task.exception ?: Exception("Não foi possível alterar o evento.")))
        }
    }

    override fun createEvent(event: Event, onResult: (Result<Unit>) -> Unit) {
        val document = firestore.collection("events").document()
        document.set(event.copy(id = document.id))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(
                        task.exception ?: Exception("Não foi possível criar o evento.")
                    ))
                }
            }
    }

    override fun getOrganizerEvents(
        organizerId: String,
        onResult: (Result<List<Event>>) -> Unit
    ) {
        firestore.collection("events")
            .whereEqualTo("organizerId", organizerId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val events = task.result.documents.mapNotNull { document ->
                        document.toObject(Event::class.java)?.copy(id = document.id)
                    }
                    onResult(Result.success(events))
                } else {
                    onResult(Result.failure(
                        task.exception ?: Exception("Não foi possível carregar seus eventos.")
                    ))
                }
            }
    }

    override fun getEventById(
        eventId: String,
        onResult: (Result<Event>) -> Unit
    ) {
        firestore.collection("events")
            .document(eventId)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar o evento.")

                    onResult(Result.failure(exception))
                    return@addOnCompleteListener
                }

                val document = task.result
                val event = document
                    ?.takeIf { it.exists() }
                    ?.toObject(Event::class.java)
                    ?.copy(id = document.id)

                if (event != null) {
                    onResult(Result.success(event))
                } else {
                    onResult(Result.failure(Exception("Evento não encontrado.")))
                }
            }
    }

    override fun getEvents(
        onResult: (Result<List<Event>>) -> Unit
    ) {
        firestore.collection("events")
            .orderBy("startAt", Query.Direction.ASCENDING)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val events = task.result
                        ?.documents
                        ?.mapNotNull { document ->
                            document.toObject(Event::class.java)
                                ?.copy(id = document.id)
                        }
                        .orEmpty()

                    onResult(Result.success(events))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar os eventos.")

                    onResult(Result.failure(exception))
                }
            }
    }
}
