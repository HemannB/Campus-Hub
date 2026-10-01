package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class EventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getEvents(
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
