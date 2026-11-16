package com.example.spott.drafts

data class DraftInput(
    val location: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val notes: String = ""
)

data class DraftErrors(
    val location: String? = null,
    val latitude: String? = null,
    val longitude: String? = null
)

data class ValidatedDraft(
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String
)

data class DraftValidation(val value: ValidatedDraft?, val errors: DraftErrors)

object DraftValidator {
    fun validate(input: DraftInput): DraftValidation {
        val location = input.location.trim()
        val errors = DraftErrors(
            location = if (location.isBlank()) "Enter a location or address." else null,
            latitude = coordinateError(input.latitude, input.longitude, 90, "latitude"),
            longitude = coordinateError(input.longitude, input.latitude, 180, "longitude")
        )
        return DraftValidation(
            if (errors == DraftErrors()) ValidatedDraft(
                location, input.latitude.trim().toDoubleOrNull(), input.longitude.trim().toDoubleOrNull(), input.notes.trim()
            ) else null,
            errors
        )
    }

    private fun coordinateError(text: String, other: String, limit: Int, name: String): String? {
        if (text.isBlank()) return if (other.isNotBlank()) "Enter $name too." else null
        val number = text.trim().toDoubleOrNull()
        return if (number == null || !number.isFinite() || number < -limit || number > limit) {
            "Enter $name from -$limit to $limit using a decimal dot."
        } else null
    }
}
