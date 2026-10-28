package com.iot.widget.ui.guide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.GuideRepository
import com.iot.widget.data.OnboardingRepository
import com.iot.widget.domain.guide.GuideTopic
import com.iot.widget.domain.guide.search
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI layer · User Guide state and ViewModels: topic list with search, and one topic.
 */
data class GuideUiState(val query: String = "", val topics: List<GuideTopic> = emptyList(), val loaded: Boolean = false)

/** UI layer · the User Guide topic list with search (spec §6.4). */
@HiltViewModel
class GuideViewModel @Inject constructor(
    guide: GuideRepository,
    private val onboarding: OnboardingRepository,
) : ViewModel() {

    private val queryText = MutableStateFlow("")

    val uiState: StateFlow<GuideUiState> = combine(flow { emit(guide.topics()) }, queryText) { all, q ->
        GuideUiState(query = q, topics = all.search(q), loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuideUiState())

    /**
     * What is typed, available at once. The field reads this, not [uiState]: uiState waits for
     * the topics to load, and a text field fed from a delayed flow loses keys or jumps the cursor.
     */
    val query: StateFlow<String> = queryText.asStateFlow()

    fun setQuery(value: String) {
        queryText.value = value
    }

    /** "Show welcome tour": clearing the flag makes AppRoot show the tour right away. */
    fun showTour() {
        viewModelScope.launch { onboarding.reset() }
    }
}

/** Loading, an id that is not in the guide (e.g. an old link), or the topic. */
sealed interface GuideTopicState {
    data object Loading : GuideTopicState
    data object NotFound : GuideTopicState
    data class Loaded(val topic: GuideTopic) : GuideTopicState
}

/** UI layer · one topic, by its id from the route. */
@HiltViewModel
class GuideTopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    guide: GuideRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle.get<String>("id"))

    val state: StateFlow<GuideTopicState> = flow {
        val topic = guide.topics().firstOrNull { it.id == id }
        emit(if (topic == null) GuideTopicState.NotFound else GuideTopicState.Loaded(topic))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuideTopicState.Loading)
}
