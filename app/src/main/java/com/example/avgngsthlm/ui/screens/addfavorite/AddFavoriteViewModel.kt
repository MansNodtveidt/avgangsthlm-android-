package com.example.avgngsthlm.ui.screens.addfavorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.remote.model.Departure
import com.example.avgngsthlm.data.remote.model.LineInfo
import com.example.avgngsthlm.data.remote.model.StopLocation
import com.example.avgngsthlm.data.repository.DeparturesRepository
import com.example.avgngsthlm.data.repository.FavoriteRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WizardStep { STOP, LINE, DIRECTION, NAME }

data class AddFavoriteUiState(
    val step: WizardStep = WizardStep.STOP,

    // Steg 1 — Hållplats
    val stopQuery: String = "",
    val stopResults: List<StopLocation> = emptyList(),
    val selectedStop: StopLocation? = null,
    val isSearchingStop: Boolean = false,

    // Steg 2 — Linje (extraheras från avgångarna)
    val availableLines: List<LineInfo> = emptyList(),
    val selectedLine: LineInfo? = null,
    val isLoadingLines: Boolean = false,

    // Steg 3 — Riktning
    val availableDirections: List<String> = emptyList(),
    val selectedDirection: String? = null,
    val isLoadingDirections: Boolean = false,

    // Steg 4 — Namn
    val favoriteName: String = "",

    // Gemensamt
    val error: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val stepNumber: Int get() = step.ordinal + 1
    val totalSteps: Int get() = 4
}

@OptIn(FlowPreview::class)
class AddFavoriteViewModel(app: Application) : AndroidViewModel(app) {

    private val favoriteRepo = FavoriteRepository(AppDatabase.getInstance(app).favoriteDao())
    private val departuresRepo = DeparturesRepository()

    private val _uiState = MutableStateFlow(AddFavoriteUiState())
    val uiState: StateFlow<AddFavoriteUiState> = _uiState.asStateFlow()

    private val _stopQuery = MutableStateFlow("")
    val stopQuery: StateFlow<String> = _stopQuery.asStateFlow()

    // Cache för avgångar — undviker dubbelt API-anrop i steg 2→3
    private var cachedDepartures: List<Departure> = emptyList()

    init {
        viewModelScope.launch {
            _stopQuery
                .debounce(350)
                .filter { it.length >= 2 }
                .collect { query -> searchStops(query) }
        }
    }

    // ── Steg 1 ────────────────────────────────────────────────

    fun onStopQueryChange(query: String) {
        _stopQuery.value = query
        _uiState.update { it.copy(stopQuery = query, selectedStop = null, stopResults = emptyList()) }
    }

    fun selectStop(stop: StopLocation) {
        _stopQuery.value = stop.name
        _uiState.update { it.copy(selectedStop = stop, stopResults = emptyList()) }
    }

    private fun searchStops(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingStop = true, error = null) }
            departuresRepo.searchLocations(query)
                .onSuccess { stops ->
                    _uiState.update { it.copy(stopResults = stops, isSearchingStop = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSearchingStop = false, error = "Kunde inte söka hållplatser: ${e.message}") }
                }
        }
    }

    fun proceedFromStop() {
        val stop = _uiState.value.selectedStop ?: return
        _uiState.update { it.copy(step = WizardStep.LINE, isLoadingLines = true, error = null, availableLines = emptyList()) }
        viewModelScope.launch {
            departuresRepo.fetchRawDepartures(stop.id)
                .onSuccess { departures ->
                    cachedDepartures = departures
                    val lines = departuresRepo.extractLines(departures)
                    if (lines.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoadingLines = false,
                                error = "Inga avgångar hittades för denna hållplats just nu. Försök igen senare."
                            )
                        }
                    } else {
                        _uiState.update { it.copy(availableLines = lines, isLoadingLines = false) }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingLines = false, error = "Kunde inte hämta linjer: ${e.message}")
                    }
                }
        }
    }

    // ── Steg 2 ────────────────────────────────────────────────

    fun selectLine(line: LineInfo) {
        _uiState.update { it.copy(selectedLine = line) }
    }

    fun proceedFromLine() {
        val line = _uiState.value.selectedLine ?: return
        _uiState.update {
            it.copy(step = WizardStep.DIRECTION, isLoadingDirections = true, error = null, availableDirections = emptyList())
        }
        viewModelScope.launch {
            // Använd cachade avgångar om möjligt, annars hämta igen
            val departures = if (cachedDepartures.isNotEmpty()) {
                cachedDepartures
            } else {
                val stop = _uiState.value.selectedStop ?: return@launch
                departuresRepo.fetchRawDepartures(stop.id)
                    .getOrElse { e ->
                        _uiState.update { it.copy(isLoadingDirections = false, error = "Kunde inte hämta riktningar: ${e.message}") }
                        return@launch
                    }
                    .also { cachedDepartures = it }
            }

            val directions = departuresRepo.extractDirections(departures, line.lineNumber)
            if (directions.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingDirections = false,
                        error = "Inga riktningar hittades för linje ${line.lineNumber}. API:et kanske inte returnerade tillräckligt med avgångar."
                    )
                }
            } else {
                _uiState.update { it.copy(availableDirections = directions, isLoadingDirections = false) }
            }
        }
    }

    // ── Steg 3 ────────────────────────────────────────────────

    fun selectDirection(direction: String) {
        _uiState.update { it.copy(selectedDirection = direction) }
    }

    fun proceedFromDirection() {
        val state = _uiState.value
        val suggestion = buildString {
            state.selectedStop?.name?.let { append(it) }
            state.selectedLine?.lineNumber?.let { append(" · Linje $it") }
            state.selectedDirection?.let { append(" mot $it") }
        }
        _uiState.update { it.copy(step = WizardStep.NAME, favoriteName = suggestion) }
    }

    // ── Steg 4 ────────────────────────────────────────────────

    fun onNameChange(name: String) {
        _uiState.update { it.copy(favoriteName = name) }
    }

    fun saveFavorite() {
        val state = _uiState.value
        val stop = state.selectedStop ?: return
        if (state.favoriteName.isBlank()) {
            _uiState.update { it.copy(error = "Ange ett namn för favoriten") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            favoriteRepo.insert(
                Favorite(
                    name = state.favoriteName.trim(),
                    siteId = stop.id,
                    stopName = stop.name,
                    lineFilter = state.selectedLine?.lineNumber,
                    directionFilter = state.selectedDirection
                )
            )
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }

    // ── Navigation ────────────────────────────────────────────

    fun goBack() {
        val prev = when (_uiState.value.step) {
            WizardStep.LINE -> WizardStep.STOP
            WizardStep.DIRECTION -> WizardStep.LINE
            WizardStep.NAME -> WizardStep.DIRECTION
            WizardStep.STOP -> WizardStep.STOP
        }
        _uiState.update { it.copy(step = prev, error = null) }
    }

    fun retryCurrentStep() {
        _uiState.update { it.copy(error = null) }
        when (_uiState.value.step) {
            WizardStep.LINE -> proceedFromStop()
            WizardStep.DIRECTION -> proceedFromLine()
            else -> Unit
        }
    }

    fun isFirstStep(): Boolean = _uiState.value.step == WizardStep.STOP
}
