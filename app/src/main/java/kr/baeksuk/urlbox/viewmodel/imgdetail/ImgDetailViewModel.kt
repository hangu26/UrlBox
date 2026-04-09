package kr.baeksuk.urlbox.viewmodel.imgdetail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.domain.DeleteImageUseCase
import kr.baeksuk.urlbox.domain.ToggleFavoriteUseCase
import kr.baeksuk.urlbox.util.util.UserSessionManager

class ImgDetailViewModel(
    application: Application,
    private val sessionManager: UserSessionManager,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteImageUseCase: DeleteImageUseCase
) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnDelete = MutableLiveData<Boolean>()
    val btnDelete = _btnDelete

    private val _btnLoadUrl = MutableLiveData<Boolean>()
    val btnLoadUrl = _btnLoadUrl

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnEdit() {
        _btnEditState.value = true
    }

    fun btnDelete() {
        _btnDelete.value = true
    }

    fun btnFavorite() {
        _btnFavoriteState.value = true
    }

    fun deleteImage(url: String, imageKey: String) {
        viewModelScope.launch {
            deleteImageUseCase(url, imageKey)
        }
    }

    fun toggleFavorite(url : String, isFavorite : Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(url, isFavorite)
        }
    }

}