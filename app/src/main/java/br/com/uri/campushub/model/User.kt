package br.com.uri.campushub.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val course: String = "",
    val semester: String = "",
    val photoUrl: String = "",
    val role: String = "STUDENT"
)
