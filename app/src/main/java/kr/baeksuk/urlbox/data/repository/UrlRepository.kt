package kr.baeksuk.urlbox.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

class UrlRepository(application: Application) : ViewModel(){

    private val urlDatabase = UrlDatabase.getInstance(application)
    private val urlDao : UrlDao = urlDatabase.urlDao()
    private val url: LiveData<List<UrlEntity>> = urlDao.getAll()

    fun insert(urlEntity : UrlEntity){
        viewModelScope.launch(Dispatchers.IO){
            try {
                urlDao.insert(urlEntity)
            }catch (e: java.lang.Exception){

            }
        }
    }

    fun update(urlEntity: UrlEntity){
        viewModelScope.launch(Dispatchers.IO){
            try {
                urlDao.update(urlEntity.urlLink, urlEntity.imageKey)
            }catch (e: java.lang.Exception){

            }
        }
    }

    fun updateFavorite(url : String, isFavorite : Boolean){
        viewModelScope.launch(Dispatchers.IO){
            try {
                urlDao.updateFavorite(url, isFavorite)
            }catch (e: java.lang.Exception){

            }
        }
    }

    fun deleteGuestData(url : String){

        viewModelScope.launch(Dispatchers.IO){
            try {
                urlDao.deleteKeyword(url)
            }catch (e: java.lang.Exception){

            }
        }

    }

    fun getGuestUrl() : LiveData<List<UrlEntity>>{
        return url
    }

}