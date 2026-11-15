package br.com.uri.campushub.model

import br.com.uri.campushub.model.Event
import java.net.URI

object EventValidator {
    fun validate(event: Event): String? {
        if (event.title.isBlank()) return "Informe o título."
        if (event.description.isBlank()) return "Informe a descrição."
        if (event.category.isBlank()) return "Informe a categoria."
        if (event.location.isBlank()) return "Informe o endereço completo."
        if (event.startAt == null || event.endAt == null) return "Informe o início e o fim."
        if (event.endAt <= event.startAt) return "O fim deve ser posterior ao início."
        if (event.maxParticipants <= 0) return "Informe uma capacidade maior que zero."
        if (event.imageUrl.isNotBlank()) {
            val uri = runCatching { URI(event.imageUrl.trim()) }.getOrNull()
            if (uri?.scheme !in listOf("https", "http") || uri?.host.isNullOrBlank()) {
                return "Informe uma URL de imagem válida (http ou https)."
            }
        }
        return null
    }
}
