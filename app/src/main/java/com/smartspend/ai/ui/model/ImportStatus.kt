package com.smartspend.ai.ui.model

sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Importing : ImportStatus
    data class Complete(val found: Int, val merged: Int = 0) : ImportStatus
    data class Failed(val message: String) : ImportStatus
}
