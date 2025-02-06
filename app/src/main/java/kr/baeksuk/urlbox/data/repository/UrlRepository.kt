package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url
import java.io.File

class UrlRepository(application: Application) : AndroidViewModel(application) {

    private val urlDatabase = UrlDatabase.getInstance(application)
    private val urlDao: UrlDao = urlDatabase.urlDao()
    private val url: LiveData<List<UrlEntity>> = urlDao.getAll()
    private val urlBackup: LiveData<List<UrlBackupEntity>> = urlDao.getAllBackup()
    private val pref = application.getSharedPreferences("User", Context.MODE_PRIVATE)
    private lateinit var databaseReference: DatabaseReference
    private var database: DatabaseReference = Firebase.database.reference

    fun insert(urlEntity: UrlEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insert(urlEntity)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 삽입 처리", e.toString())
            }
        }
    }

    fun insertBackup(urlBackupEntity: UrlBackupEntity, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insertBackup(urlBackupEntity)
                val userId = pref.getString("userId", "")
                val storageRef = FirebaseStorage.getInstance().reference.child("images/${userId}/")

                val userRef: DatabaseReference = database.child("User").child(userId!!).child("url")
                val urlLink = urlBackupEntity.urlLink

                val url = Url(
                    urlBackupEntity.urlLink,
                    urlBackupEntity.imageKey,
                    urlBackupEntity.imgUri,
                    urlBackupEntity.favorite,
                    urlBackupEntity.timeStamp
                )

                userRef.orderByChild("url").equalTo(urlLink)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {

                                Log.e("데이터 중복 여부", "중복되는 링크가 존재함")

                            } else {

                                userRef.child("img" + url.timeStamp)
                                    .setValue(url).addOnCompleteListener {
                                        Log.e("데이터 저장 여부", "저장되었습니다.")

                                    }.addOnFailureListener {
                                        Log.e("데이터 저장 여부", "저장되지 않았습니다.")
                                    }
                                Log.e("데이터 중복 여부", "중복되는 링크가 존재하지 않음")

                                val fileUri = Uri.fromFile(file) // File을 Uri로 변환
                                val fileRef = storageRef.child(file.name) // 저장할 파일 경로 설정

                                fileRef.putFile(fileUri).addOnSuccessListener {

                                    fileRef.downloadUrl.addOnSuccessListener { uri ->

                                        viewModelScope.launch(Dispatchers.IO) {
                                            try {
                                                /** 스토리지에 이미지를 업로드함과 동시에 백업 Room에 이미지 uri를 업데이트 **/
                                                urlDao.insertImgUri(uri.toString(), urlLink)
                                            } catch (e: java.lang.Exception) {

                                            }
                                        }
                                        Log.i("FirebaseStorage", "Image uploaded. URI: $uri")
                                        // 업로드된 이미지의 URI를 사용하여 추가 작업을 할 수 있음
                                    }

                                    Log.d("Storage Upload", "파일 업로드 성공: ${file.name}")
                                }.addOnFailureListener {
                                    Log.e(
                                        "Storage Upload",
                                        "파일 업로드 실패: ${file.name}, 오류: ${it.message}"
                                    )
                                }

                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

            } catch (e: java.lang.Exception) {
                Log.e("데이터 삽입 처리", e.toString())
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

    fun update(urlEntity: UrlEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.update(urlEntity.urlLink, urlEntity.imageKey)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    fun updateBackup(urlBackupEntity: UrlBackupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateBackup(urlBackupEntity.urlLink, urlBackupEntity.imageKey)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }


    fun updateFavorite(url: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateFavorite(url, isFavorite)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    fun deleteGuestData(url: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUrl(url)
            } catch (e: java.lang.Exception) {
                Log.e("삭제 처리", e.toString())
            }
        }

    }

    /** 룸에 저장된 백업 데이터와 파이어베이스에 존재하는 데이터 두개 모두 삭제 **/
    fun deleteUserData(url: String, imageKey: String) {
        val userId = pref.getString("userId", "")
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${userId}/")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUserUrl(url)

                val userId = pref.getString("userId", "")
                databaseReference =
                    FirebaseDatabase.getInstance().reference.child("User").child(userId!!)
                        .child("url")

                databaseReference.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (child in snapshot.children) {
                                    child.ref.removeValue().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.i("deleteKeyword", "success delete keyword")
                                        } else {
                                            Log.e("deleteKeyword fail", "fail delete keyword")
                                        }
                                    }
                                }
                            } else {
                                Log.e("deleteKeyword", "URL not found in Firebase")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase Error", "Failed to read data: ${error.message}")
                        }
                    })

                storageRef.child("${imageKey}.png").delete()
                    .addOnCompleteListener {
                        Log.e("스토리지 이미지 삭제 여부", "성공")
                    }.addOnFailureListener {
                        Log.e("스토리지 이미지 삭제 여부", "실패")
                    }


            } catch (e: java.lang.Exception) {
                Log.e("삭제 처리", e.toString())
            }
        }

    }

    fun getGuestUrl(): LiveData<List<UrlEntity>> {
        return url
    }

    fun getUserUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return urlBackup
    }

}