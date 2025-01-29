package kr.baeksuk.urlbox.viewmodel.nav

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url

class UrlDataViewModel() : ViewModel() {

    val urlData = MutableLiveData<List<UrlEntity>>()

    fun sendUrlCount(url : List<UrlEntity>){
        urlData.value = url
    }

}