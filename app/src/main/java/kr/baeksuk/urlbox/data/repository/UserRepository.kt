package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.User
import java.io.File
import java.io.FileOutputStream

class UserRepository(application: Application) : AndroidViewModel(application) {

    private var database: DatabaseReference = Firebase.database.reference
    private val pref = application.getSharedPreferences("User", Context.MODE_PRIVATE)
    val ONE_MEGABYTE: Long = 1024 * 1024 // 1MB
    private val urlDatabase = UrlDatabase.getInstance(application)

    private val urlDao: UrlDao = urlDatabase.urlDao()
    val ctx = application

    /**
    fun getUrlData(): LiveData<Pair<List<Url>, List<Bitmap>>> {
    val userId = pref.getString("userId", "")
    val databaseReference =
    FirebaseDatabase.getInstance().reference.child("User").child(userId!!).child("url")

    val mutableUrl = MutableLiveData<Pair<List<Url>, List<Bitmap>>>()

    // Firebase Storage 참조 가져오기
    val storage = FirebaseStorage.getInstance()

    databaseReference.addValueEventListener(object : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {
    val urlDataList = mutableListOf<Url>()
    val imgList = mutableListOf<Bitmap>()

    // 이미지 다운로드 완료 카운트 변수
    var loadedImagesCount = 0
    val totalImagesCount = snapshot.childrenCount.toInt()

    for (dataSnapshot in snapshot.children) {
    val url = dataSnapshot.child("url").value.toString()
    val imageKey = dataSnapshot.child("imageKey").value.toString()
    val favorite = dataSnapshot.child("favorite").value.toString().toBoolean()
    val timeStamp = dataSnapshot.child("timeStamp").value.toString().toLong()
    val storageReference =
    storage.reference.child("images").child(userId).child("$imageKey.png")

    Log.e("데이터 확인용", "URL: $url, imageKey: $imageKey")

    storageReference.getBytes(ONE_MEGABYTE) // 최대 1MB까지 다운로드
    .addOnSuccessListener { bytes ->

    // 다운로드한 데이터를 Bitmap으로 변환
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    imgList.add(bitmap)
    urlDataList.add(Url(url, imageKey, favorite, timeStamp))
    Log.e("데이터 확인용", "URL: $url, urlDataList: $urlDataList")

    // 이미지 다운로드 완료 시, 카운트 증가
    loadedImagesCount++

    // 모든 이미지가 다운로드되었으면 LiveData 업데이트
    if (loadedImagesCount == totalImagesCount) {
    mutableUrl.value = Pair(urlDataList, imgList)
    }

    val directory = ctx.filesDir // 앱의 내부 저장소 디렉토리
    val file = File(directory, "$imageKey.png")

    try {
    val outStream = FileOutputStream(file)
    bitmap.compress(
    Bitmap.CompressFormat.PNG,
    100,
    outStream
    ) // 압축해서 저장
    outStream.flush()
    outStream.close()
    Log.d("파일 저장", "이미지 저장 완료: ${file.absolutePath}")

    } catch (e: Exception) {
    e.printStackTrace()
    }

    }
    .addOnFailureListener { exception ->
    exception.printStackTrace()
    }
    }
    }

    override fun onCancelled(error: DatabaseError) {
    // 실패 처리
    }
    })

    return mutableUrl
    }
     **/


    fun getUrlData(): LiveData<Pair<List<Url>, List<String>>> {
        val userId = pref.getString("userId", "")
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("User").child(userId!!).child("url")

        val mutableUrl = MutableLiveData<Pair<List<Url>, List<String>>>()

        // Firebase Storage 참조 가져오기
        val storage = FirebaseStorage.getInstance()

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val urlDataList = mutableListOf<Url>()
                val imageUrls = mutableListOf<String>()

                // 이미지 다운로드 완료 카운트 변수
                var loadedImagesCount = 0
                val totalImagesCount = snapshot.childrenCount.toInt()

                for (dataSnapshot in snapshot.children) {
                    val url = dataSnapshot.child("url").value.toString()
                    val imageKey = dataSnapshot.child("imageKey").value.toString()
                    val favorite = dataSnapshot.child("favorite").value.toString().toBoolean()
                    val timeStamp = dataSnapshot.child("timeStamp").value.toString().toLong()

                    // Firebase Storage에서 이미지 URL 가져오기
                    val storageReference =
                        storage.reference.child("images").child(userId).child("$imageKey.png")

                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        // URL을 리스트에 추가

                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                /** 스토리지에 이미지를 업로드함과 동시에 백업 Room에 이미지 uri를 업데이트 **/
                                urlDao.insertImgUri(uri.toString(), url)
                            } catch (e: java.lang.Exception) {

                            }
                        }

                        imageUrls.add(uri.toString())


                        // UrlEntity 객체를 생성하여 urlDataList에 추가
                        urlDataList.add(Url(url, imageKey, uri.toString(), favorite, timeStamp))

                        // 이미지 다운로드 완료 시, 카운트 증가
                        loadedImagesCount++

                        // 모든 이미지가 다운로드되었으면 LiveData 업데이트
                        if (loadedImagesCount == totalImagesCount) {
                            mutableUrl.value = Pair(urlDataList, imageUrls)
                        }
                    }
                        .addOnFailureListener { exception ->
                            exception.printStackTrace()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 실패 처리
            }
        })

        return mutableUrl
    }


    fun insertUserId(user: User) {
        val userRef: DatabaseReference = database.child("User").child(user.userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 로그인 했을 때 db에 존재하는 아이디라면
                } else {
                    userRef.setValue(user).addOnSuccessListener {
                        Log.d("유저 아이디 저장", "유저 아이디 저장 성공")
                    }.addOnFailureListener {
                        Log.d("유저 아이디 저장", "유저 아이디 저장 실패")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 예외 처리 필요
            }
        })

    }

    fun insertAllData(user: User, urlList: List<Url>, imgFileList: List<File>) {
        val userRef: DatabaseReference = database.child("User").child(user.userId)
        val urlInUser: DatabaseReference = userRef.child("url")
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${user.userId}/")

        /**
        urlInUser.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
        if (snapshot.exists()) {
        // 기존 URL 데이터를 가져오기
        val existingUrls =
        snapshot.child("url").children.mapNotNull { it.getValue(Url::class.java) }

        // 새로운 URL 중에서 기존 데이터와 중복되지 않은 URL만 필터링
        val newUrls =
        urlList.filter { newUrl -> existingUrls.none { it.url == newUrl.url } }

        if (newUrls.isNotEmpty()) {

        newUrls.forEach { newUrl ->
        urlInUser.child("img" + newUrl.timeStamp)
        .setValue(newUrl) // ✅ `imageKey`를 키로 저장
        }

        } else {
        Log.e("URL 데이터 저장", "모든 URL이 중복되어 저장하지 않음")
        }
        } else {
        urlList.forEach { newUrl ->
        urlInUser.child(newUrl.imageKey).setValue(newUrl) // ✅ `imageKey`를 키로 저장
        }
        }
        }

        override fun onCancelled(error: DatabaseError) {
        Log.e("Firebase Error", "데이터 읽기 실패: ${error.message}")
        }
        })
         **/

        urlInUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 기존 URL 데이터 가져오기
                    val existingUrls = snapshot.children.mapNotNull { it.getValue(Url::class.java) }

                    // 새로운 URL 중 기존 데이터와 중복되지 않은 URL만 필터링
                    val newUrls = urlList.filter { newUrl ->
                        existingUrls.none { it.url == newUrl.url } // ✅ URL이 겹치면 추가 안 함 (favorite 값 무시)
                    }

                    if (newUrls.isNotEmpty()) {
                        newUrls.forEach { newUrl ->
                            urlInUser.child("img"+newUrl.timeStamp).setValue(newUrl)
                        }
                        Log.d("URL 데이터 저장", "새로운 URL 데이터 저장 완료")
                    } else {
                        Log.e("URL 데이터 저장", "모든 URL이 중복되어 저장하지 않음")
                    }
                } else {
                    // 기존 데이터가 없을 때는 바로 삽입
                    urlList.forEach { newUrl ->
                        urlInUser.child(newUrl.imageKey).setValue(newUrl)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "데이터 읽기 실패: ${error.message}")
            }
        })


        // 🔥 이미지 파일 리스트를 Firebase Storage에 업로드
        imgFileList.forEach { file ->
            if (file.exists()) {
                val fileUri = Uri.fromFile(file) // File을 Uri로 변환
                val fileRef = storageRef.child(file.name) // 저장할 파일 경로 설정

                fileRef.putFile(fileUri).addOnSuccessListener {
                    Log.d("Storage Upload", "파일 업로드 성공: ${file.name}")
                }.addOnFailureListener {
                    Log.e("Storage Upload", "파일 업로드 실패: ${file.name}, 오류: ${it.message}")
                }
            } else {
                Log.e("Storage Upload", "파일이 존재하지 않음: ${file.absolutePath}")
            }
        }

    }


}