package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.domain.LoadThumbnailDataUseCase
import kr.baeksuk.urlbox.view.nav.ThumbnailState

class ThumbnailViewModel(
    private val application: Application,
    private val loadThumbnailDataUseCase: LoadThumbnailDataUseCase
) : AndroidViewModel(application){

    private val _thumbnailState = MutableLiveData<ThumbnailState>()
    val thumbnailState: LiveData<ThumbnailState> = _thumbnailState

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState

    fun btnAdd() {
        _btnAddState.value = true
    }

    fun loadThumbnail() {

        viewModelScope.launch {

            val isLoggedIn = loadThumbnailDataUseCase.isLoggedIn()

            if (isLoggedIn){

                val urls = loadThumbnailDataUseCase.getLoginUrlBackup().value.orEmpty()
                val tags = loadThumbnailDataUseCase.getLoginTagBackup().value.orEmpty()
                _thumbnailState.value = ThumbnailState.Login(urls, tags)
            }else{

                val urls = loadThumbnailDataUseCase.getGuestUrls().value.orEmpty()
                _thumbnailState.value = ThumbnailState.Guest(urls)

            }

        }
    }

}