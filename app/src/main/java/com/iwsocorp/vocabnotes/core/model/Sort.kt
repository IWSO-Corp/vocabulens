package com.iwsocorp.vocabnotes.core.model

enum class SortBy(val column: String) {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    WORD("word")
}

enum class SortOrder(val value: String) {
    ASC("ASC"),
    DESC("DESC")
}