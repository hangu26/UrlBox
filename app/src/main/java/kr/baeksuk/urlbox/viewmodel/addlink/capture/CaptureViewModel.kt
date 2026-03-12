package kr.baeksuk.urlbox.viewmodel.addlink.capture

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.UserTags
import java.io.File

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val urlDatabase = UrlDatabase.getInstance(application)

    private val urlDao: UrlDao = urlDatabase.urlDao()
    private val _repo = UrlRepository(application)
    private val urlBackup = _repo.getUserUrlBackup()
    private val _userRepo = UserRepository(application)

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnCaptureState = MutableLiveData<Boolean>()
    val btnCaptureState = _btnCaptureState

    private val _btnSaveState = MutableLiveData<Boolean>()
    val btnSaveState = _btnSaveState

    private val _btnSkipState = MutableLiveData<Boolean>()
    val btnSkipState = _btnSkipState

    private val _btnCancelState = MutableLiveData<Boolean>()
    val btnCancelState = _btnCancelState

    private val _btnShowTagsState = MutableLiveData<Boolean>()
    val btnShowTagsState = _btnShowTagsState

    private val _btnAddTagsStage = MutableLiveData<Boolean>()
    val btnAddTagsStage = _btnAddTagsStage

    var isClicked = 0

    fun getTagData(lifecycleOwner: LifecycleOwner) : LiveData<List<Tag>>{
        val mutableTag = MutableLiveData<List<Tag>>()
        _userRepo.getTagData().observe(lifecycleOwner){
            mutableTag.value = it
        }

        return mutableTag
    }

    fun btnShowTags() {
        _btnShowTagsState.value = isClicked % 2 == 0
        isClicked++
    }

    fun btnSkip() {
        _btnSkipState.value = true
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnCapture() {
        _btnCaptureState.value = true
    }

    fun btnSave() {
        _btnSaveState.value = true
    }

    fun btnCancel() {
        _btnCancelState.value = true
    }

    fun btnAddTags(){
        _btnAddTagsStage.value = false
    }

    fun insertUrl(urlEntity: UrlEntity, url: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUrl = urlDao.getUrlIsExist(url)
            withContext(Dispatchers.Main) {
                if (existingUrl == null) {
                    _repo.insert(urlEntity)
                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun insertBackupUrl(
        urlBackupEntity: UrlBackupEntity,
        url: String,
        context: Context,
        file: File,
        tag : String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUrl = urlDao.getBackupUrlIsExist(url)
            Log.e("저장된지 확인", "URL: $url, 존재 여부: ${existingUrl != null}")
            withContext(Dispatchers.Main) {
                if (existingUrl == null) {
                    _repo.insertBackup(urlBackupEntity, file, tag)
                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    /** 태그 복수 저장 가능 함수 **/
    fun insertBackupUrlMultipleTags(
        urlBackupEntity: UrlBackupEntity,
        url: String,
        context: Context,
        file: File,
        tags: List<UserTags>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUrl = urlDao.getBackupUrlIsExist(url)
            withContext(Dispatchers.Main) {
                if (existingUrl == null) {
                    _repo.insertBackupMultipleTags(urlBackupEntity, file, tags)
                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateUrl(urlEntity: UrlEntity, url: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.update(urlEntity)
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()

            }

        }
    }

    fun updateBackupUrl(urlBackupEntity: UrlBackupEntity, context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateBackup(urlBackupEntity, file)
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()

            }

        }
    }

}