package com.dotkios.ulaaa.ui.bucketlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.BucketItem
import com.dotkios.ulaaa.data.repository.BucketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BucketViewModel @Inject constructor(
    private val repository: BucketRepository,
) : ViewModel() {

    val items: StateFlow<List<BucketItem>> = repository.items.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun add(name: String, location: String, note: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.add(name, location, note) }
    }

    fun remove(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }
}
