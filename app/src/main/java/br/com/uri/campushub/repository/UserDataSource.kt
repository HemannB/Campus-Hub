package br.com.uri.campushub.repository

import br.com.uri.campushub.model.User

interface UserDataSource {
    fun updateProfile(
        userId: String,
        name: String,
        studentId: String,
        course: String,
        semester: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun createProfile(
        user: User,
        onResult: (Result<Unit>) -> Unit
    )

    fun getProfile(
        userId: String,
        onResult: (Result<User>) -> Unit
    )
}
