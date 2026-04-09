package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.domain.LoadThumbnailDataUseCase
import kr.baeksuk.urlbox.view.nav.ThumbnailState

class ThumbnailViewModel(
    application: Application,
    private val loadThumbnailDataUseCase: LoadThumbnailDataUseCase
) : AndroidViewModel(application) {

    private val _thumbnailState = MutableLiveData<ThumbnailState>()
    val thumbnailState: LiveData<ThumbnailState> = _thumbnailState

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState: LiveData<Boolean> = _btnAddState

    fun btnAdd() {
        _btnAddState.value = true
    }

    fun loadThumbnail() {
        viewModelScope.launch {
            _thumbnailState.value = loadThumbnailDataUseCase()
        }
    }
}