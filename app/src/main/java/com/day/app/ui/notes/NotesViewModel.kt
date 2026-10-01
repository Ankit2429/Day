package com.day.app.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NotesTab(val label: String) {
    NOTES("NOTES"),
    HIGHLIGHTS("HIGHLIGHTS"),
    FAVORITES("FAVORITES")
}

enum class NoteSortOrder(val label: String) {
    ALL_NOTES("All Notes"),
    RECENTLY_UPDATED("Recently Updated"),
    PINNED("Pinned Notes"),
    ALPHABETICAL("Alphabetical")
}

data class NoteCategoryInfo(
    val name: String,
    val count: Int,
    val iconType: String = "folder"
)

data class NotesFilterState(
    val tab: NotesTab = NotesTab.NOTES,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val isCategoriesViewActive: Boolean = false,
    val sortOrder: NoteSortOrder = NoteSortOrder.ALL_NOTES,
    val customCategories: Set<String> = setOf("Design", "Robotics", "Research", "Ideas")
)

data class NotesUiState(
    val allNotes: List<Note> = emptyList(),
    val displayedNotes: List<Note> = emptyList(),
    val pinnedNotes: List<Note> = emptyList(),
    val regularNotes: List<Note> = emptyList(),
    val categories: List<NoteCategoryInfo> = emptyList(),
    val selectedTab: NotesTab = NotesTab.NOTES,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val isCategoriesViewActive: Boolean = false,
    val sortOrder: NoteSortOrder = NoteSortOrder.ALL_NOTES
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val noteRepository = app.noteRepository

    private val _filterState = MutableStateFlow(NotesFilterState())

    val uiState: StateFlow<NotesUiState> = combine(
        noteRepository.getAllNotes(),
        _filterState
    ) { allNotes: List<Note>, filter: NotesFilterState ->

        // 1. Compute categories from tags in all notes + custom categories
        val allTags = mutableMapOf<String, Int>()
        filter.customCategories.forEach { allTags[it] = 0 }
        allNotes.forEach { note ->
            note.tags.forEach { tag ->
                val trimmed = tag.trim()
                if (trimmed.isNotBlank()) {
                    allTags[trimmed] = (allTags[trimmed] ?: 0) + 1
                }
            }
        }
        val categoryList = allTags.map { (name, count) ->
            NoteCategoryInfo(
                name = name,
                count = count,
                iconType = when (name.lowercase()) {
                    "robotics", "code", "dev" -> "code"
                    "ideas", "brainstorm" -> "lightbulb"
                    "design", "wireframe", "ui" -> "design"
                    else -> "folder"
                }
            )
        }.sortedByDescending { it.count }

        // 2. Filter by search query
        var filtered = if (filter.searchQuery.isNotBlank()) {
            allNotes.filter { note ->
                note.title.contains(filter.searchQuery, ignoreCase = true) ||
                        note.body.contains(filter.searchQuery, ignoreCase = true) ||
                        note.tags.any { it.contains(filter.searchQuery, ignoreCase = true) }
            }
        } else {
            allNotes
        }

        // 3. Filter by category
        if (filter.selectedCategory != null) {
            filtered = filtered.filter { note ->
                note.tags.any { it.equals(filter.selectedCategory, ignoreCase = true) }
            }
        }

        // 4. Filter by tab
        val tabFiltered = when (filter.tab) {
            NotesTab.NOTES -> filtered
            NotesTab.HIGHLIGHTS -> {
                val highlights = filtered.filter { 
                    it.body.contains(">") || it.body.contains("**") || it.body.contains("<u>") || it.tags.any { t -> t.contains("highlight", ignoreCase = true) }
                }
                if (highlights.isEmpty()) filtered else highlights
            }
            NotesTab.FAVORITES -> filtered.filter { it.pinned }
        }

        // 5. Sort
        val sorted = when (filter.sortOrder) {
            NoteSortOrder.ALL_NOTES -> tabFiltered.sortedByDescending { it.updatedAt }
            NoteSortOrder.RECENTLY_UPDATED -> tabFiltered.sortedByDescending { it.updatedAt }
            NoteSortOrder.PINNED -> tabFiltered.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
            NoteSortOrder.ALPHABETICAL -> tabFiltered.sortedBy { it.title.lowercase() }
        }

        val pinned = sorted.filter { it.pinned }
        val regular = sorted.filter { !it.pinned }

        NotesUiState(
            allNotes = allNotes,
            displayedNotes = sorted,
            pinnedNotes = pinned,
            regularNotes = regular,
            categories = categoryList,
            selectedTab = filter.tab,
            selectedCategory = filter.selectedCategory,
            searchQuery = filter.searchQuery,
            isSearchExpanded = filter.isSearchExpanded,
            isCategoriesViewActive = filter.isCategoriesViewActive,
            sortOrder = filter.sortOrder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun setTab(tab: NotesTab) {
        _filterState.update { it.copy(tab = tab, isCategoriesViewActive = false) }
    }

    fun setCategory(category: String?) {
        _filterState.update { it.copy(selectedCategory = category, isCategoriesViewActive = false) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(expanded: Boolean) {
        _filterState.update {
            it.copy(
                isSearchExpanded = expanded,
                searchQuery = if (!expanded) "" else it.searchQuery
            )
        }
    }

    fun toggleCategoriesView() {
        _filterState.update { it.copy(isCategoriesViewActive = !it.isCategoriesViewActive) }
    }

    fun setSortOrder(order: NoteSortOrder) {
        _filterState.update { it.copy(sortOrder = order) }
    }

    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            noteRepository.togglePin(noteId)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _filterState.update { it.copy(customCategories = it.customCategories + trimmed) }
        }
    }
}
