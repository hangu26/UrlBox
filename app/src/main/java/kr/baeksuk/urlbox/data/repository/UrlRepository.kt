package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UrlInTag
import kr.baeksuk.urlbox.model.UserTags
import java.io.File

class UrlRepository(application: Application) : AndroidViewModel(application) {

    private val urlDatabase = UrlDatabase.getInstance(application)
    private val urlDao: UrlDao = urlDatabase.urlDao()
    private val url: LiveData<List<UrlEntity>> = urlDao.getAll()
    private val urlBackup: LiveData<List<UrlBackupEntity>> = urlDao.getAllBackup()
    private val tagBackup: LiveData<List<TagBackupEntity>> = urlDao.getTagBackup()
    private val pref = application.getSharedPreferences("User", Context.MODE_PRIVATE)
    private lateinit var databaseReference: DatabaseReference
    private var database: DatabaseReference = Firebase.database.reference

    /** Room에 있는 백업 데이터 url 에 태그 추가 함수 **/
    fun insertUserTag(tag: String, urlTitle: String) {
        // 현재 시간 저장
        val timeStamp = System.currentTimeMillis()

        // 새로운 UserTags 객체 생성
        val userTag = UserTags(tag = tag, timeStamp = timeStamp)

        // viewModelScope에서 비동기 작업 시작
        viewModelScope.launch(Dispatchers.IO) {
            // URL에 해당하는 UrlBackupEntity를 가져옴
            val urlBackupEntity = urlDao.getUrlBackupByTitle(urlTitle)
            val tagBackupEntity = urlDao.getTagBackupByTitle(tag)

            // UrlBackupEntity가 존재하면
            if (urlBackupEntity != null) {
                // 기존 tag 리스트 가져오기
                val currentTags = urlBackupEntity.tag?.toMutableList() ?: mutableListOf()

                if (currentTags.none { it.tag == tag }) {
                    // 새로운 UserTags 추가
                    currentTags.add(userTag)

                    // 업데이트된 tag 리스트를 URL 엔티티에 반영하여 업데이트

                    if (userTag.tag?.isNotBlank() == true) {

                        urlDao.updateUserTags(currentTags, urlTitle)

                        insertUserTagInFirebase(userTag, urlTitle)

                    }
                }
            }


            if (tagBackupEntity != null) {
                val urlListInTag = tagBackupEntity.urlList?.toMutableList() ?: mutableListOf()
                if (urlListInTag.none { it == urlTitle }) {
                    urlListInTag.add(urlTitle)

                    // DAO가 String만 받으므로 Gson으로 변환
                    val urlListAsString = Gson().toJson(urlListInTag)
                    urlDao.updateUserUrlInTags(urlListAsString, tag)
                }
            } else {
                val newTagBackup = TagBackupEntity(
                    tag = tag,
                    urlList = listOf(urlTitle)
                )
                // insert는 List<TagBackupEntity>여야 하므로 리스트로 감싸서 호출
                urlDao.insertTagBackup(listOf(newTagBackup))
            }

        }
    }

    /** 파이어베이스에 있는 데이터 url 에 태그 추가 함수 **/
    private fun insertUserTagInFirebase(userTag: UserTags, urlTitle: String) {

        val userId = pref.getString("userId", "")

        val userTagRef: DatabaseReference = database.child("User").child(userId!!).child("url")
        val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")

        userTagRef.orderByChild("url").equalTo(urlTitle)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        for (child in snapshot.children) {

                            val key = child.key

                            userTagRef.child(key!!).child("tags").child("tag" + userTag.timeStamp)
                                .setValue(userTag).addOnCompleteListener {
                                    Log.e("데이터 저장 여부", "태그가 저장되었습니다.")
                                }.addOnFailureListener {
                                    Log.e("데이터 저장 여부", "태그가 저장되지 않았습니다.")
                                }

                        }

                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        updateTagInTagFirebase(userTag.tag!!, tagRef, urlTitle)

    }

    /** url 에 태그 삭제 함수 **/

    fun deleteUserUrlTag(tag: String, urlTitle: String) {

        // viewModelScope에서 비동기 작업 시작
        viewModelScope.launch(Dispatchers.IO) {
            // URL에 해당하는 UrlBackupEntity를 가져옴
            val urlBackupEntity = urlDao.getUrlBackupByTitle(urlTitle)
            val tagBackupEntity = urlDao.getTagBackupByTitle(tag)

            if (urlBackupEntity != null) {
                // 기존 tag 리스트 가져오기
                val currentTags = urlBackupEntity.tag?.toMutableList() ?: mutableListOf()

                val tagToRemove = currentTags.find { it.tag == tag }

                if (tagToRemove != null) {
                    currentTags.remove(tagToRemove)

                    urlDao.updateUserTags(currentTags, urlTitle)
                    deleteUserTagInFirebase(tag, urlTitle)
                }

            }

            if (tagBackupEntity != null) {

                val urlListInTag = tagBackupEntity.urlList?.toMutableList() ?: mutableListOf()

                val urlToRemove = urlListInTag.find { it == urlTitle }

                if (urlToRemove != null) {

                    urlListInTag.remove(urlToRemove)

                    val urlListAsString = Gson().toJson(urlListInTag)

                    urlDao.updateUserUrlInTags(urlListAsString, tag)

                }
            }


        }
    }

    private fun deleteUserTagInFirebase(tag: String, urlTitle: String) {
        val userId = pref.getString("userId", "") ?: return
        val userTagRef: DatabaseReference = database.child("User").child(userId).child("url")
        val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")

        // 1. [Tag] 노드에서 해당 태그를 찾고 그 안의 URL 삭제
        tagRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tagSnapshot in snapshot.children) {
                    // 핵심 수정: 현재 순회 중인 태그의 이름이 내가 삭제하려는 tag(A)와 같은지 확인
                    val currentTagName = tagSnapshot.child("tag").getValue(String::class.java)

                    if (currentTagName == tag) { // 클릭한 태그 이름과 일치할 때만 진입
                        val urlsSnapshot = tagSnapshot.child("url")
                        for (urlChild in urlsSnapshot.children) {
                            val urlValue = urlChild.child("url").getValue(String::class.java) ?: continue

                            if (urlValue == urlTitle) {
                                urlChild.ref.removeValue().addOnSuccessListener {
                                    Log.d("삭제완료", "태그[$tag] 내에서 URL 삭제됨: $urlValue")
                                }
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. [url] 노드에서 해당 URL을 찾고 그 안의 태그 리스트 중 해당 태그 삭제
        userTagRef.orderByChild("url").equalTo(urlTitle)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val urlKey = child.key ?: continue

                        // 해당 URL 내의 tags 리스트 중 이름이 tag(A)인 것만 삭제
                        child.child("tags").ref.orderByChild("tag").equalTo(tag)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(tagInUrlSnapshot: DataSnapshot) {
                                    for (data in tagInUrlSnapshot.children) {
                                        data.ref.removeValue().addOnSuccessListener {
                                            Log.d("삭제완료", "URL 내의 태그 리스트에서 [$tag] 삭제됨")
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun insert(urlEntity: UrlEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insert(urlEntity)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 삽입 처리", e.toString())
            }
        }
    }

    fun insertBackup(urlBackupEntity: UrlBackupEntity, file: File, tag: String) {

        val tagData = listOf(
            TagBackupEntity(
                tag = tag,
                count = "1",
                timeStamp = System.currentTimeMillis().toString(),
                urlList = listOf(urlBackupEntity.urlLink)
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insertBackup(urlBackupEntity)

                insertTagBackup(tagData, tag)

                val userId = pref.getString("userId", "")
                Log.e("유저 아이디 확인", userId!!)
                val storageRef = FirebaseStorage.getInstance().reference.child("images/${userId}/")

                val userRef: DatabaseReference = database.child("User").child(userId!!).child("url")
                val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")
                val urlLink = urlBackupEntity.urlLink

                val url = Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    timeStamp = urlBackupEntity.timeStamp,
                    urlName = urlBackupEntity.urlName,
//                    tag = urlBackupEntity.tag
                )

                val gson = Gson()

                // List<UserTagsEntity>를 JSON 문자열로 변환
                val tagJson =
                    gson.toJson(urlBackupEntity.tag)  // List<UserTagsEntity>? -> JSON String

                // UserTagsEntity 객체 생성
                val userTags = UserTags(
                    tag = tag, timeStamp = urlBackupEntity.timeStamp
                )


                /**
                if (tag != "") {

                tagRef.orderByChild("tag").equalTo(tag)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                val urlInTag = UrlInTag(url = url.url)

                if (snapshot.exists()) {
                for (child in snapshot.children) {
                val key = child.key
                val timeStamp = System.currentTimeMillis()

                val tagCountRef = tagRef.child(key!!).child("count")
                tagCountRef.runTransaction(object :
                Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                var tagCount = data.getValue(Int::class.java) ?: 0
                tagCount++
                data.value = tagCount
                return Transaction.success(data)
                }

                override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
                ) {

                }
                })

                val tagUrlRef = tagRef.child(key).child("url")
                tagUrlRef.equalTo(url.url)
                .addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                } else {
                tagUrlRef.child("url$timeStamp")
                .setValue(urlInTag)
                .addOnCompleteListener {
                Log.e("데이터 저장 여부", "태그가 저장되었습니다.")
                }.addOnFailureListener {
                Log.e(
                "데이터 저장 여부",
                "태그가 저장되지 않았습니다."
                )
                }
                }
                }

                override fun onCancelled(error: DatabaseError) {

                }
                })


                Log.e("데이터 중복 여부", "중복되는 태그가 존재함")
                }

                } else {
                val timeStamp = System.currentTimeMillis().toString()

                val tagInfo = Tag(tag,"1",timeStamp)

                tagRef.child("tag" + url.timeStamp).child("tag").setValue(tagInfo)

                tagRef.child("tag" + url.timeStamp).child("url")
                .child("url$timeStamp").setValue(urlInTag)

                //                                    tagRef.child("tag" + url.timeStamp).child("tag").setValue(tag)
                //
                //                                    tagRef.child("tag" + url.timeStamp).child("timeStamp")
                //                                        .setValue(timeStamp)
                //
                //                                    tagRef.child("tag" + url.timeStamp).child("count").setValue(1)

                }
                }

                override fun onCancelled(error: DatabaseError) {

                }
                })
                }
                 **/
                updateTagInTagFirebase(tag, tagRef, url.url)

                userRef.orderByChild("url").equalTo(urlLink)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {

                                Log.e("데이터 중복 여부", "중복되는 링크가 존재함")

                            } else {

                                userRef.child("img" + url.timeStamp).setValue(url)
                                    .addOnCompleteListener {
                                        Log.e("데이터 저장 여부", "저장되었습니다.")

                                    }.addOnFailureListener {
                                        Log.e("데이터 저장 여부", "저장되지 않았습니다.")
                                    }

                                userRef.child("img" + url.timeStamp).child("tags")
                                    .child("tag" + url.timeStamp).setValue(userTags)

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

    private fun insertTagBackup(tagBackupEntity: List<TagBackupEntity>, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {

            if (tag.isNotEmpty()) {
                // 1️⃣ 저장하려는 태그 리스트 가져오기
                val urlTagList = tagBackupEntity.map { it.tag }

                // 2️⃣ 이미 존재하는 태그 가져오기 (DB에서 조회)
                val existingTags = urlDao.getTagBackupByTags(urlTagList).associateBy { it.tag }

                // 3️⃣ 새로운 태그 & 업데이트할 태그 분리
                val newTags = mutableListOf<TagBackupEntity>()
                val tagsToUpdate = mutableListOf<TagBackupEntity>()

                for (tagEntity in tagBackupEntity) {
                    val existingTag = existingTags[tagEntity.tag]

                    if (existingTag != null) {
                        // 🔥 이미 존재하는 태그 → urlList 업데이트
                        val updatedUrls =
                            (existingTag.urlList.orEmpty() + tagEntity.urlList.orEmpty()).distinct()
                        val updatedTagEntity = existingTag.copy(urlList = updatedUrls)
                        tagsToUpdate.add(updatedTagEntity)
                    } else {
                        // 🔥 없는 태그 → 새로 추가
                        newTags.add(tagEntity)
                    }
                }

                // 4️⃣ 중복되지 않은 태그만 삽입
                if (newTags.isNotEmpty()) {
                    urlDao.insertTagBackup(newTags)
                }

                // 5️⃣ 기존 태그는 URL 리스트만 업데이트
                if (tagsToUpdate.isNotEmpty()) {
                    urlDao.updateUrlInTags(tagsToUpdate)
                }
            }

        }
    }

    private fun updateTagInTagFirebase(tag: String, tagRef: DatabaseReference, urlLink: String) {

        if (tag.isNotBlank()) {

            tagRef.orderByChild("tag").equalTo(tag)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val urlInTag = UrlInTag(url = urlLink)

                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val key = child.key
                                val timeStamp = System.currentTimeMillis()

                                val tagCountRef = tagRef.child(key!!).child("count")
                                tagCountRef.runTransaction(object : Transaction.Handler {
                                    override fun doTransaction(data: MutableData): Transaction.Result {
                                        var tagCount = data.getValue(Int::class.java) ?: 0
                                        tagCount++
                                        data.value = tagCount
                                        return Transaction.success(data)
                                    }

                                    override fun onComplete(
                                        error: DatabaseError?,
                                        committed: Boolean,
                                        currentData: DataSnapshot?
                                    ) {

                                    }
                                })

                                val tagUrlRef = tagRef.child(key).child("url")
                                tagUrlRef.equalTo(urlLink)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (snapshot.exists()) {

                                            } else {
                                                tagUrlRef.child("url$timeStamp").setValue(urlInTag)
                                                    .addOnCompleteListener {
                                                        Log.e("데이터 저장 여부", "태그가 저장되었습니다.")
                                                    }.addOnFailureListener {
                                                        Log.e(
                                                            "데이터 저장 여부", "태그가 저장되지 않았습니다."
                                                        )
                                                    }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                        }
                                    })


                                Log.e("데이터 중복 여부", "중복되는 태그가 존재함")
                            }

                        } else {
                            val timeStamp = System.currentTimeMillis().toString()

                            val tagInfo = Tag(tag, "1", timeStamp)

                            tagRef.child("tag$timeStamp").setValue(tagInfo)

                            tagRef.child("tag$timeStamp").child("url").child("url$timeStamp")
                                .setValue(urlInTag)

                            /**
                            tagRef.child("tag" + url.timeStamp).child("tag").setValue(tag)

                            tagRef.child("tag" + url.timeStamp).child("timeStamp")
                            .setValue(timeStamp)

                            tagRef.child("tag" + url.timeStamp).child("count").setValue(1)
                             **/

                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }


    }

    fun updateGuestUrlInfo(url: String, urlName: String, urlMemo: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateGuestUrlInfo(url, urlName, urlMemo)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }

    }

    fun updateUrlInfo(url: String, urlName: String, urlMemo: String) {

        val userId = pref.getString("userId", "")

        viewModelScope.launch(Dispatchers.IO) {

            try {

                urlDao.updateUrlInfo(url, urlName, urlMemo)

                databaseReference =
                    FirebaseDatabase.getInstance().reference.child("User").child(userId!!)
                        .child("url")

                databaseReference.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {

                            if (snapshot.exists()) {

                                for (child in snapshot.children) {
                                    val key = child.key

                                    if (key != null) {
                                        databaseReference.child(key).child("urlName")
                                            .setValue(urlName).addOnCompleteListener {
                                                Log.e("업데이트 성공", "URL 이름 변경 완료")
                                            }.addOnFailureListener { e ->
                                                Log.e("업데이트 실패", e.toString())
                                            }

                                        databaseReference.child(key).child("urlMemo")
                                            .setValue(urlMemo).addOnCompleteListener {
                                                Log.e("업데이트 성공", "URL 메모 변경 완료")
                                            }.addOnFailureListener { e ->
                                                Log.e("업데이트 실패", e.toString())
                                            }

                                    }

                                }

                            } else {

                            }

                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

            } catch (e: java.lang.Exception) {

            }

        }

    }

    fun update(urlEntity: UrlEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.update(urlEntity.urlLink, urlEntity.imageKey)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    fun updateBackup(urlBackupEntity: UrlBackupEntity, file: File) {

        val userId = pref.getString("userId", "") ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$userId/")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateBackup(urlBackupEntity.urlLink, urlBackupEntity.imageKey)

                val databaseReference =
                    FirebaseDatabase.getInstance().reference.child("User").child(userId)
                        .child("url")

                databaseReference.orderByChild("url").equalTo(urlBackupEntity.urlLink)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val child = snapshot.children.firstOrNull() ?: run {
                                Log.e("데이터 없음", "해당 URL을 가진 데이터가 없습니다.")
                                return
                            }

                            val key = child.key ?: return
                            val oldImageKey = child.child("imageKey").getValue(String::class.java)
                            Log.e("기존 이미지 키", oldImageKey.toString())

                            // 기존 이미지 삭제
                            storageRef.child("${oldImageKey}.png").delete().addOnCompleteListener {
                                    Log.e("중복 이미지 삭제 여부", "성공")
                                }.addOnFailureListener {
                                    Log.e("중복 이미지 삭제 여부", "실패")
                                }

                            // 새 이미지 key 저장
                            databaseReference.child(key).child("imageKey")
                                .setValue(urlBackupEntity.imageKey).addOnCompleteListener {
                                    Log.e("업데이트 성공", "사진 변경 완료")
                                }.addOnFailureListener { e ->
                                    Log.e("업데이트 실패", e.toString())
                                }

                            val fileUri = Uri.fromFile(file)
                            val fileRef = storageRef.child(file.name)

                            fileRef.putFile(fileUri).addOnSuccessListener {
                                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                                        viewModelScope.launch(Dispatchers.IO) {

                                            try {
                                                urlDao.insertImgUri(
                                                    uri.toString(), urlBackupEntity.urlLink
                                                )
                                            } catch (e: Exception) {
                                                Log.e("Room 업데이트 실패", e.toString())
                                            }
                                        }

                                        Log.i("FirebaseStorage", "Image uploaded. URI: $uri")
                                    }
                                    Log.d("Storage Upload", "파일 업로드 성공: ${file.name}")
                                }.addOnFailureListener {
                                    Log.e(
                                        "Storage Upload",
                                        "파일 업로드 실패: ${file.name}, 오류: ${it.message}"
                                    )
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase 에러", error.message)
                        }
                    })
            } catch (e: Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    /** 🔥 기존 Firebase Storage 이미지 삭제 함수 */
    private fun deleteOldImage(storageRef: StorageReference, imageKey: String) {
        val oldImageRef = storageRef.child(imageKey)
        oldImageRef.delete().addOnSuccessListener {
                Log.d("FirebaseStorage", "기존 이미지 삭제 성공: $imageKey")
            }.addOnFailureListener {
                Log.e("FirebaseStorage", "기존 이미지 삭제 실패: $imageKey, 오류: ${it.message}")
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

    fun updateUserFavorite(url: String, isFavorite: Boolean) {

        val userId = pref.getString("userId", "")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateUserFavorite(url, isFavorite)

                databaseReference =
                    FirebaseDatabase.getInstance().reference.child("User").child(userId!!)
                        .child("url")

                databaseReference.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {

                                for (child in snapshot.children) {
                                    val key = child.key

                                    if (key != null) {
                                        databaseReference.child(key).child("favorite")
                                            .setValue(isFavorite).addOnCompleteListener {
                                                Log.e("업데이트 성공", "Firebase favorite 업데이트 완료")
                                            }.addOnFailureListener { e ->
                                                Log.e("업데이트 실패", e.toString())
                                            }
                                    }

                                }

                            } else {

                                Log.e("데이터 없음", "해당 URL을 가진 데이터가 없습니다.")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase 에러", error.message)
                        }
                    })

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

    /** 전체 태그 삭제 시, 게시물에 따른 태그들 삭제 함수(TagActivity)
     * 룸에 저장된 백업 데이터와 파이어베이스에 존재하는 데이터 두개 모두 삭제 **/

    fun deleteUserTag(tag: String) {
        val userId = pref.getString("userId", "") ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {

                urlDao.deleteTag(tag)
                val rootRef = FirebaseDatabase.getInstance().reference.child("User").child(userId)
                val tagRef = rootRef.child("Tag")
                val urlMainRef = rootRef.child("url")

                tagRef.orderByChild("tag").equalTo(tag).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (tagSnapshot in snapshot.children) {
                            val linkedUrlsNode = tagSnapshot.child("url")

                            linkedUrlsNode.children.forEach { urlChild ->

                                val realUrlString = urlChild.child("url").getValue(String::class.java)

                                if (realUrlString != null) {
                                    deleteUserUrlTag(tag, realUrlString)

                                    urlMainRef.orderByChild("url").equalTo(realUrlString)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(imgSnapshot: DataSnapshot) {
                                                imgSnapshot.children.forEach { matchingImg ->

                                                    val tagsNode = matchingImg.child("tags")
                                                    tagsNode.children.forEach { bTag ->
                                                        if (bTag.child("tag").value == tag) {
                                                            bTag.ref.removeValue().addOnSuccessListener {
                                                                Log.i("deleteTag", "B태그 삭제 완료: ${matchingImg.key}")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                }
                            }

                            tagSnapshot.ref.removeValue().addOnSuccessListener {
                                Log.i("deleteTag", "A태그(수탉) 삭제 성공")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            } catch (e: Exception) {
                Log.e("deleteTag", e.toString())
            }
        }
    }

    fun deleteUserData(url: String, imageKey: String) {
        val userId = pref.getString("userId", "")
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${userId}/")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUserUrl(url)

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

                storageRef.child("${imageKey}.png").delete().addOnCompleteListener {
                        Log.e("스토리지 이미지 삭제 여부", "성공")
                    }.addOnFailureListener {
                        Log.e("스토리지 이미지 삭제 여부", "실패")
                    }


            } catch (e: java.lang.Exception) {
                Log.e("삭제 처리", e.toString())
            }
        }

    }

    fun deleteUserBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUserBackup()
            } catch (e: java.lang.Exception) {
                Log.e("로그아웃 시, 백업 데이터 전체 삭제", e.toString())
            }
        }
    }

    fun deleteUserTagBackup() {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUserTagBackup()
            } catch (e: java.lang.Exception) {
                Log.e("로그아웃 시, 백업 데이터 전체 삭제", e.toString())
            }
        }

    }

    fun getGuestUrl(): LiveData<List<UrlEntity>> {
        return url
    }

    fun getUserUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return urlBackup
    }

    fun getUserTagBackup(): LiveData<List<TagBackupEntity>> {
        return tagBackup
    }

    suspend fun hasBackupData(): Boolean {
        return urlDao.hasBackupData() // suspend 함수 호출
    }

}