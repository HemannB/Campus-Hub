package br.com.uri.campushub.repository

import br.com.uri.campushub.model.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun updateProfile(
        userId: String,
        name: String,
        studentId: String,
        course: String,
        semester: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val profileUpdates = mapOf(
            "name" to name,
            "studentId" to studentId,
            "course" to course,
            "semester" to semester
        )

        firestore.collection("users")
            .document(userId)
            .update(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível atualizar o perfil.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun createProfile(
        user: User,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection("users")
            .document(user.id)
            .set(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível salvar o perfil.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun getProfile(
        userId: String,
        onResult: (Result<User>) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.toObject(User::class.java)

                    if (user != null) {
                        onResult(Result.success(user))
                    } else {
                        onResult(
                            Result.failure(
                                Exception("Perfil não encontrado.")
                            )
                        )
                    }
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar o perfil.")

                    onResult(Result.failure(exception))
                }
            }
    }
}
