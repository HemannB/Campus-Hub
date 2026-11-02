package br.com.uri.campushub.viewmodel

import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.CurrentUserDataSource
import br.com.uri.campushub.repository.UserDataSource

class FakeCurrentUserDataSource(
    var userId: String? = "user-1"
) : CurrentUserDataSource {
    override fun currentUserId(): String? = userId
}

class FakeUserDataSource : UserDataSource {
    data class UpdateRequest(
        val userId: String,
        val name: String,
        val studentId: String,
        val course: String,
        val semester: String
    )

    var requestedUserId: String? = null
    var updateRequest: UpdateRequest? = null
    var createdUser: User? = null
    private var profileResult: ((Result<User>) -> Unit)? = null
    private var updateResult: ((Result<Unit>) -> Unit)? = null
    private var createResult: ((Result<Unit>) -> Unit)? = null

    override fun updateProfile(
        userId: String,
        name: String,
        studentId: String,
        course: String,
        semester: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        updateRequest = UpdateRequest(userId, name, studentId, course, semester)
        updateResult = onResult
    }

    override fun createProfile(
        user: User,
        onResult: (Result<Unit>) -> Unit
    ) {
        createdUser = user
        createResult = onResult
    }

    override fun getProfile(
        userId: String,
        onResult: (Result<User>) -> Unit
    ) {
        requestedUserId = userId
        profileResult = onResult
    }

    fun completeProfile(result: Result<User>) {
        checkNotNull(profileResult)(result)
    }

    fun completeUpdate(result: Result<Unit>) {
        checkNotNull(updateResult)(result)
    }

    fun completeCreate(result: Result<Unit>) {
        checkNotNull(createResult)(result)
    }
}
