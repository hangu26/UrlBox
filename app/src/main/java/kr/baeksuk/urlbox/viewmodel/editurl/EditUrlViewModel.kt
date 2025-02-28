package kr.baeksuk.urlbox.viewmodel.editurl

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository

class EditUrlViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)

    private val _btnChangeImgState = MutableLiveData<Boolean>()
    val btnChangeImgState = _btnChangeImgState

    private val _btnSaveChangesState = MutableLiveData<Boolean>()
    val btnSaveChangesState = _btnSaveChangesState

    private val _btnBackState = MutableLiveData<Boolean>()
    val btnBackState = _btnBackState

    private val _btnSetTagState = MutableLiveData<Boolean>()
    val btnSetTagState = _btnSetTagState

    fun btnSetTag(){
        _btnSetTagState.value = true
    }

    fun btnBack(){
        _btnBackState.value = true
    }

    fun btnChangeImg() {
        _btnChangeImgState.value = true
    }

    fun btnSaveChanges(){
        _btnSaveChangesState.value = true
    }

    fun updateUserUrl(url: String, urlName : String, urlMemo : String, context: Context) {

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateUrlInfo(url, urlName, urlMemo)
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()

            }

        }
    }

    fun updateGuestUrl(url: String, urlName : String, urlMemo : String, context: Context){

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateGuestUrlInfo(url, urlName, urlMemo)
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()

            }

        }

    }

}