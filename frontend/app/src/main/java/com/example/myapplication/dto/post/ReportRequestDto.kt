package com.example.myapplication.dto.post

enum class ReportReason {
    INNAPROPRIATE_CONTENT,
    WRONG_LOCALISATION,
    SPAM,
    PRIVATE_LIFE_VIOLATION,
    DANGEROUS_INFORMATIONS,
    OTHER
}

data class ReportRequestDto(
    val reason: ReportReason,
    val details: String? = null
)
