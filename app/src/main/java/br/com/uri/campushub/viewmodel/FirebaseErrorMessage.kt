package br.com.uri.campushub.viewmodel

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

internal fun Throwable.toUserMessage(defaultMessage: String): String {
    val causes = generateSequence(this) { error ->
        error.cause?.takeUnless { cause -> cause === error }
    }.toList()

    causes.filterIsInstance<FirebaseNetworkException>().firstOrNull()?.let {
        return "Sem conexão com a internet. Tente novamente."
    }

    causes.filterIsInstance<FirebaseAuthException>().firstOrNull()?.let { error ->
        return when (error.errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INVALID_EMAIL",
            "ERROR_WRONG_PASSWORD" -> "E-mail ou senha inválidos."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já está cadastrado."
            "ERROR_WEAK_PASSWORD" -> "A senha informada é muito fraca."
            "ERROR_USER_DISABLED" -> "Esta conta foi desativada."
            "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas. Tente novamente mais tarde."
            else -> defaultMessage
        }
    }

    causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()?.let { error ->
        return when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Você não tem permissão para realizar esta operação."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "O serviço está indisponível. Tente novamente."
            else -> defaultMessage
        }
    }

    if (causes.any { it is FirebaseException }) {
        return defaultMessage
    }

    return causes.lastOrNull()?.message?.takeIf(String::isNotBlank)
        ?: defaultMessage
}
