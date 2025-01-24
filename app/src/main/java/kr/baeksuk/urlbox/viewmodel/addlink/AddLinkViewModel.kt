package kr.baeksuk.urlbox.viewmodel.addlink

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class AddLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState


    fun btnClose(){
        _btnCloseState.value = true
    }

    fun btnAdd(){
        _btnAddState.value = true
    }

}