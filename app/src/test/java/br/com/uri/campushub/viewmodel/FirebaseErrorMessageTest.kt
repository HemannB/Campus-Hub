package br.com.uri.campushub.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseErrorMessageTest {

    @Test
    fun `known authentication codes use friendly messages`() {
        val expectedMessages = mapOf(
            "ERROR_INVALID_CREDENTIAL" to "E-mail ou senha inválidos.",
            "ERROR_INVALID_EMAIL" to "E-mail ou senha inválidos.",
            "ERROR_WRONG_PASSWORD" to "E-mail ou senha inválidos.",
            "ERROR_EMAIL_ALREADY_IN_USE" to "Este e-mail já está cadastrado.",
            "ERROR_WEAK_PASSWORD" to "A senha informada é muito fraca.",
            "ERROR_USER_DISABLED" to "Esta conta foi desativada.",
            "ERROR_TOO_MANY_REQUESTS" to "Muitas tentativas. Tente novamente mais tarde."
        )

        expectedMessages.forEach { (errorCode, expectedMessage) ->
            assertEquals(
                expectedMessage,
                authErrorMessage(errorCode, "Mensagem padrão")
            )
        }
    }

    @Test
    fun `unknown authentication code uses default message`() {
        assertEquals(
            "Mensagem padrão",
            authErrorMessage("ERROR_UNKNOWN", "Mensagem padrão")
        )
    }

    @Test
    fun `firestore codes use friendly messages or default`() {
        assertEquals(
            "Você não tem permissão para realizar esta operação.",
            firestoreErrorMessage("PERMISSION_DENIED", "Mensagem padrão")
        )
        assertEquals(
            "O serviço está indisponível. Tente novamente.",
            firestoreErrorMessage("UNAVAILABLE", "Mensagem padrão")
        )
        assertEquals(
            "Mensagem padrão",
            firestoreErrorMessage("UNKNOWN", "Mensagem padrão")
        )
    }

    @Test
    fun `ordinary error uses deepest message or default`() {
        val nested = Exception("Falha externa", IllegalArgumentException("Campo inválido"))
        val blank = Exception("   ")

        assertEquals(
            "Campo inválido",
            nested.toUserMessage("Mensagem padrão")
        )
        assertEquals(
            "Mensagem padrão",
            blank.toUserMessage("Mensagem padrão")
        )
    }
}
