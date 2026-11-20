package app.lalla.domain.model

data class AiResponse(
    val text: String,
    val memoriesToSave: List<String> = emptyList()
)
