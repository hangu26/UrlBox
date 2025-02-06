package kr.baeksuk.urlbox.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

class UrlRepository(application: Application) : ViewModel(){

    private val urlDatabase = UrlDatabase.getInstance(application)
    private val urlDao : UrlDao = urlDatabase.urlDao()
    private val url: LiveData<List<UrlEntity>> = urlDao.getAll()
    private val urlBackup: LiveData<List<UrlBackupEntity>> = urlDao.getAllBackup()

    fun insert(urlEntity : UrlEntity){
        viewModelScope.launch(Dispatchers.IO){
            try {
                urlDao.insert(urlEntity)
            }catch (e: java.lang.Exception){

            }
        }
    }

    /** 파이어베이스에서 데이터를 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/
//    fun insertUrlBackup(urlBackupEntity : List<UrlBackupEntity>){
//        viewModelScope.launch(Dispatchers.IO){
//            try {
//                urlDao.insertUrlBackup(urlBackupEntity)
//            }catch (e: java.lang.Exception){
//
//            }
//        }
//    }

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

    fun getUserUrlBackup() : LiveData<List<UrlBackupEntity>>{
        return urlBackup
    }

}