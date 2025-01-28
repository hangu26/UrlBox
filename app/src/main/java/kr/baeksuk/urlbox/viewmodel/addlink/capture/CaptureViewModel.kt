package kr.baeksuk.urlbox.viewmodel.addlink.capture

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val urlDatabase = UrlDatabase.getInstance(application)

    private val urlDao : UrlDao = urlDatabase.urlDao()

    private val repo = UrlRepository(application)

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnCaptureState = MutableLiveData<Boolean>()
    val btnCaptureState = _btnCaptureState

    private val _btnSaveState = MutableLiveData<Boolean>()
    val btnSaveState = _btnSaveState

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnCapture() {
        _btnCaptureState.value = true
    }

    fun btnSave() {
        _btnSaveState.value = true
    }

    fun insertUrl(urlEntity: UrlEntity, url: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO){
            val existingUrl = urlDao.getUrlIsExist(url)
            withContext(Dispatchers.Main){
                if(existingUrl == null){
                    repo.insert(urlEntity)
                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(context, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

}