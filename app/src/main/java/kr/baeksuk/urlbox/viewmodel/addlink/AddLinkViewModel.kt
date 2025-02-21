package kr.baeksuk.urlbox.viewmodel.addlink

import android.app.Application
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class AddLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState

    private val _urlInputDoneState = MutableLiveData<Boolean>()
    val urlInputDoneState = _urlInputDoneState

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnAdd() {
        _btnAddState.value = true
    }

    /** EditText의 onEditorActionListener는 키보드 액션 이벤트를 처리할 때 반드시 Boolean 값을 반환해야 한다.  **/
    fun onUrlInputDone(actionId : Int) : Boolean{
        return if (actionId == EditorInfo.IME_ACTION_DONE){
            _urlInputDoneState.value = true
            true
        }else{
            _urlInputDoneState.value = false
            false
        }
    }

}