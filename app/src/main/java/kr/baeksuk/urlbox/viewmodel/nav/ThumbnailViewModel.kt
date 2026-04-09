package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kr.baeksuk.urlbox.domain.LoadThumbnailDataUseCase
import kr.baeksuk.urlbox.view.nav.ThumbnailState

class ThumbnailViewModel(
    application: Application,
    private val loadThumbnailDataUseCase: LoadThumbnailDataUseCase
) : AndroidViewModel(application) {

    val thumbnailState: StateFlow<ThumbnailState> = loadThumbnailDataUseCase.observeThumbnailState()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ThumbnailState.Guest(emptyList())
        )

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState: LiveData<Boolean> = _btnAddState

    fun btnAdd() {
        _btnAddState.value = true
    }
}