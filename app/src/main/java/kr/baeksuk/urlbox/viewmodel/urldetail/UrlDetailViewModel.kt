package kr.baeksuk.urlbox.viewmodel.urldetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.domain.DeleteImageUseCase
import kr.baeksuk.urlbox.domain.ToggleFavoriteUseCase
import kr.baeksuk.urlbox.domain.UpdateUrlLinkUseCase
import kr.baeksuk.urlbox.domain.UpdateUrlMemoUseCase
import kr.baeksuk.urlbox.domain.UpdateUrlNameUseCase
import kr.baeksuk.urlbox.util.util.UserSessionManager

class UrlDetailViewModel(
    application: Application,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateUrlNameUseCase: UpdateUrlNameUseCase,
    private val updateUrlLinkUseCase: UpdateUrlLinkUseCase,
    private val updateUrlMemoUseCase: UpdateUrlMemoUseCase,
    private val deleteImageUseCase: DeleteImageUseCase,
    private val sessionManager : UserSessionManager
) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnChangeImgState = MutableLiveData<Boolean>()
    val btnChangeImgState = _btnChangeImgState

    private val _btnDelete = MutableLiveData<Boolean>()
    val btnDelete = _btnDelete

    private val _btnLoadUrl = MutableLiveData<Boolean>()
    val btnLoadUrl = _btnLoadUrl

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnImageFullState = MutableLiveData<Boolean>()
    val btnImageFullState = _btnImageFullState

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun loadSessionState() {
        viewModelScope.launch {
            _isLoggedIn.value = sessionManager.userSession.first().autoLogin ?: false
        }
    }

    fun btnEdit() {
        _btnEditState.value = true
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnChangeImg() {
        _btnChangeImgState.value = true
    }

    fun btnDelete() {
        _btnDelete.value = true
    }

    fun btnFavorite() {
        _btnFavoriteState.value = true
    }

    fun btnImageFull() {
        _btnImageFullState.value = true
    }

    fun deleteData(url: String, imageKey: String) {

        viewModelScope.launch {
            deleteImageUseCase(url, imageKey)
        }

    }

    fun updateUrlName(url: String, urlName: String) {

        viewModelScope.launch {
            updateUrlNameUseCase(url, urlName)
        }

    }

    fun updateUrlLink(oldUrl: String, newUrl: String) {
        viewModelScope.launch {
            updateUrlLinkUseCase(oldUrl, newUrl)
        }
    }

    fun updateUrlMemo(url: String, urlMemo: String) {

        viewModelScope.launch {

            updateUrlMemoUseCase(url, urlMemo)

        }

    }

    fun toggleFavorite(url: String, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(url, isFavorite)
        }
    }

}