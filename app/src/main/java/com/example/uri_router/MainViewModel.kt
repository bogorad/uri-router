package com.example.uri_router

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).urlPatternDao()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    val patterns: StateFlow<List<UrlPattern>> =
        dao.getAllPatterns()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    init {
        // Pre-populate on first run if empty
        viewModelScope.launch {
            val current = dao.getAllPatterns().first()
            if (current.isEmpty()) {
                val defaults = listOf(
                    "google.com", "youtube.com", ".bruc", ".lan",
                    "github.com", "grok.com", "x.com", "claude.ai"
                )
                defaults.forEach { dao.insert(UrlPattern(pattern = it)) }
            }
        }
    }

    fun addPattern(patternText: String) {
        val trimmed = patternText.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val id = dao.insert(UrlPattern(pattern = trimmed))
                if (id == -1L) {
                    _events.emit(UiEvent.ShowMessage("Pattern already exists"))
                }
            } catch (e: SQLiteConstraintException) {
                _events.emit(UiEvent.ShowMessage("Pattern already exists"))
            }
        }
    }

    fun deletePattern(item: UrlPattern) {
        viewModelScope.launch {
            dao.delete(item)
        }
    }

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
    }
}

