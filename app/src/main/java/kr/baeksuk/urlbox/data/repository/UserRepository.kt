package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
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
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UrlToLogin
import kr.baeksuk.urlbox.model.User
import kr.baeksuk.urlbox.model.UserTags
import java.io.File

class UserRepository(application: Application) : AndroidViewModel(application) {

    private var database: DatabaseReference = Firebase.database.reference
    private val pref = application.getSharedPreferences("User", Context.MODE_PRIVATE)
    private val urlDatabase = UrlDatabase.getInstance(application)

    private val urlDao: UrlDao = urlDatabase.urlDao()


    fun getTagData(userId: String): LiveData<List<Tag>> {
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("User").child(userId!!).child("Tag")

        val mutableTag = MutableLiveData<List<Tag>>()

        databaseReference.orderByChild("tag")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tagDataList = mutableListOf<Tag>()

                    snapshot.children.forEach { dataSnapshot ->
                        val tag = dataSnapshot.child("tag").value.toString() // "tag" 필드의 값만 가져오기
                        val timeStamp = dataSnapshot.child("timeStamp").value.toString()

                        // URL 리스트 가져오기
                        val urlList = mutableListOf<String>()
                        dataSnapshot.child("url").children.forEach { urlSnapshot ->
                            urlSnapshot.child("url").value?.toString()?.let { urlList.add(it) }
                        }

                        if (tag !in tagDataList.map { it.tag }) {
                            tagDataList.add(
                                Tag(
                                    tag = tag,
                                    timeStamp = timeStamp,
                                    urlList = urlList
                                )
                            )
                        }
                    }

                    mutableTag.value = tagDataList
                }

                override fun onCancelled(error: DatabaseError) {
                    // 오류 처리
                }
            })

        return mutableTag
    }


    fun getUrlData(userId: String): LiveData<Pair<List<Url>, List<String>>> {
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("User").child(userId!!).child("url")

        val mutableUrl = MutableLiveData<Pair<List<Url>, List<String>>>()

        // Firebase Storage 참조 가져오기
        val storage = FirebaseStorage.getInstance()

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    data class UrlLoadResult(
                        val order: Int,
                        val urlData: Url,
                        val imgUri: String
                    )

                    val orderedResults = coroutineScope {
                        snapshot.children.mapIndexed { index, dataSnapshot ->
                            async(Dispatchers.IO) {
                                val url = dataSnapshot.child("url").value.toString()
                                val imageKey = dataSnapshot.child("imageKey").value.toString()
                                val favorite = dataSnapshot.child("favorite").value.toString().toBoolean()
                                val timeStamp = dataSnapshot.child("timeStamp").value.toString().toLong()
                                val urlName = dataSnapshot.child("urlName").value.toString()
                                val urlMemo = dataSnapshot.child("urlMemo").value.toString()

                                // 🔹 tags 가져오기
                                val tagList = mutableListOf<UserTags>()
                                val tagsSnapshot = dataSnapshot.child("tags")
                                for (tagSnapshot in tagsSnapshot.children) {
                                    val tagValue = tagSnapshot.child("tag").value.toString()
                                    tagList.add(
                                        UserTags(
                                            tag = tagValue,
                                            timeStamp = timeStamp.toString().toLong()
                                        )
                                    )
                                }

                                // Firebase Storage에서 이미지 URL 가져오기
                                val storageReference =
                                    storage.reference.child("images").child(userId).child("$imageKey.png")

                                val imgUri = try {
                                    val uri = storageReference.downloadUrl.await().toString()
                                    try {
                                        /** 스토리지에 이미지를 업로드함과 동시에 백업 Room에 이미지 uri를 업데이트 **/
                                        urlDao.insertImgUri(uri, url)
                                    } catch (e: Exception) {
                                        Log.e("Room 이미지 URI 저장 실패", "url: $url, 오류: ${e.message}")
                                    }
                                    uri
                                } catch (exception: Exception) {
                                    // Storage 404 등 실패해도 동일한 슬롯을 유지해서 url/이미지 순서를 보장
                                    Log.e(
                                        "Storage 이미지 로드 실패",
                                        "imageKey: $imageKey, url: $url, 오류: ${exception.message}"
                                    )
                                    ""
                                }

                                UrlLoadResult(
                                    order = index,
                                    urlData = Url(
                                        url,
                                        imageKey,
                                        imgUri,
                                        favorite,
                                        timeStamp,
                                        urlName,
                                        urlMemo,
                                        tagList
                                    ),
                                    imgUri = imgUri
                                )
                            }
                        }.awaitAll().sortedBy { it.order }
                    }

                    mutableUrl.value = Pair(
                        orderedResults.map { it.urlData },
                        orderedResults.map { it.imgUri }
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 실패 처리
                Log.e("Firebase DB 오류", error.message)
            }
        })

        return mutableUrl
    }


    suspend fun insertUserIdSuspend(user: User) {
        val userRef = database.child("User").child(user.userId)

        // get().await()를 사용하여 snapshot을 바로 가져옵니다.
        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {
            userRef.setValue(user).await()
        }
    }

    /**
     * 2. 전체 데이터 저장 (유저 정보 + URL 리스트 + 이미지 파일)
     */
    suspend fun insertAllDataSuspend(
        user: User,
        urlList: List<UrlToLogin>,
        imgFileList: List<File>
    ) = coroutineScope {
        val userRef = database.child("User").child(user.userId)
        val urlInUser = userRef.child("url")
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${user.userId}/")

        // 유저 존재 여부 확인 및 생성
        val snapshot = userRef.get().await()
        if (!snapshot.exists()) {
            userRef.setValue(user).await()
        }

        // URL 및 이미지 업로드 실행 (병렬 처리)
        insertUrlDataInFirebaseSuspend(urlInUser, urlList, imgFileList, storageRef)
    }

    /**
     * 3. URL 데이터 및 이미지 업로드 핵심 로직 (성능 최적화 버전)
     */
    private suspend fun insertUrlDataInFirebaseSuspend(
        urlInUser: DatabaseReference,
        urlList: List<UrlToLogin>,
        imgFileList: List<File>,
        storageRef: StorageReference
    ) = coroutineScope {

        // [STEP 1] 기존 URL 목록 한 번에 가져오기
        val existingUrlsSnapshot = urlInUser.get().await()
        val existingUrls =
            existingUrlsSnapshot.children.mapNotNull { it.getValue(UrlToLogin::class.java) }

        // 중복되지 않은 새 데이터만 필터링
        val newUrls = urlList.filter { newUrl -> existingUrls.none { it.url == newUrl.url } }

        // [STEP 2] DB 업데이트 - updateChildren을 사용하여 여러 개를 한 번의 네트워크 요청으로 처리
        val dbTask = async {
            if (newUrls.isNotEmpty()) {
                val updateMap = mutableMapOf<String, Any>()
                newUrls.forEach { newUrl ->
                    updateMap["img${newUrl.timeStamp}"] = newUrl
                }
                urlInUser.updateChildren(updateMap).await()
            }
        }

        // [STEP 3] Storage 업로드 - 모든 파일을 동시에 업로드 시작 (병렬 처리)
        val storageTasks = imgFileList.filter { it.exists() }.map { file ->
            async {
                val fileUri = Uri.fromFile(file)
                storageRef.child(file.name).putFile(fileUri).await()
            }
        }

        // [STEP 4] 모든 작업(DB + Storage)이 끝날 때까지 대기
        dbTask.await()
        storageTasks.awaitAll()
    }

}
