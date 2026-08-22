package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.graphics.BitmapFactory
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.PreparationTag
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
    private lateinit var databaseReference: DatabaseReference
    private var database: DatabaseReference = Firebase.database.reference

    /** Room에 있는 백업 데이터 url 에 태그 추가 함수 **/
    fun insertUserTag(tag: String, urlTitle: String, userId: String) {
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

                        insertUserTagInFirebase(userTag, urlTitle, userId)

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
                    urlList = listOf(urlTitle),
                    firebaseTagId = "tag$timeStamp"
                )
                // insert는 List<TagBackupEntity>여야 하므로 리스트로 감싸서 호출
                urlDao.insertTagBackup(listOf(newTagBackup))
            }

        }
    }

    /** insertPreparationTag ?? */
    fun insertPreparationTag(tag: String, url: String) {
        val prepTag = PreparationTag(
            tag = tag,
            timeStamp = System.currentTimeMillis(),
        )

        viewModelScope.launch(Dispatchers.IO) {
            urlDao.insertPreparationTag(prepTag)
        }
    }

    /** clearPreparationTags ??? */
    fun clearPreparationTags() {
        viewModelScope.launch(Dispatchers.IO) {
            urlDao.clearPreparationTags()
        }
    }

    /** deletePreparationTag ?? */
    fun deletePreparationTag(tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            urlDao.deletePreparationTag(tag)
        }
    }

    /** getPreparationTags ??? ?? */
    fun getPreparationTags(): LiveData<List<PreparationTag>> = urlDao.getPreparationTags()

    /** 파이어베이스에 있는 데이터 url 에 태그 추가 함수 **/

    /** Delete all hidden URLs for a user (Firebase + Room + Storage) */
    fun deleteAllHiddenUrls(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userUrlRef = database.child("User").child(userId).child("url")
                val snapshot = userUrlRef.get().await()
                snapshot.children.forEach { childSnapshot ->
                    val hiddenVal = childSnapshot.child("hidden").getValue(Boolean::class.java) ?: false
                    if (hiddenVal) {
                        // delete associated image from Firebase Storage
                        try {
                            val imageKey = childSnapshot.child("imageKey").getValue(String::class.java)
                            val imagePath = childSnapshot.child("imagePath").getValue(String::class.java)
                            val imgUri = childSnapshot.child("imgUri").getValue(String::class.java)
                            deleteStorageImageByMetadata(
                                userId = userId,
                                imageKey = imageKey,
                                imagePath = imagePath,
                                imgUri = imgUri
                            )
                        } catch (ie: Exception) {
                            Log.e("UrlRepository", "Error deleting hidden url storage image: ${ie.message}")
                        }

                        // remove from Firebase DB
                        try {
                            childSnapshot.ref.removeValue().await()
                        } catch (de: Exception) {
                            Log.e("UrlRepository", "Failed to remove hidden url DB node: ${de.message}")
                        }
                    }
                }
                // remove local hidden entries
                urlDao.deleteAllHiddenUrlBackups()
                urlDao.deleteAllHiddenUrls()
            } catch (e: Exception) {
                Log.e("UrlRepository", "Failed to delete hidden urls: ${e.message}")
            }
        }
    }
    private fun insertUserTagInFirebase(userTag: UserTags, urlTitle: String, userId: String) {

        val userTagRef: DatabaseReference = database.child("User").child(userId).child("url")
        val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")

        userTagRef.orderByChild("url").equalTo(urlTitle)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                /** onDataChange */
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

                /** onCancelled */
                override fun onCancelled(error: DatabaseError) {

                }
            })

        updateTagInTagFirebase(
            tag = userTag.tag!!,
            tagRef = tagRef,
            urlLink = urlTitle,
            preferredTagId = "tag${userTag.timeStamp}"
        )

    }

    /** url 에 태그 삭제 함수 **/

    fun deleteUserUrlTag(tag: String, urlTitle: String, userId: String) {

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
                    deleteUserTagInFirebase(tag, urlTitle, userId)
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

    /** deleteUserTagInFirebase ?? */
    private fun deleteUserTagInFirebase(tag: String, urlTitle: String, userId: String) {
        val userTagRef: DatabaseReference = database.child("User").child(userId).child("url")
        val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")

        // 1. [Tag] 노드에서 해당 태그를 찾고 그 안의 URL 삭제
        tagRef.addListenerForSingleValueEvent(object : ValueEventListener {
            /** onDataChange */
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tagSnapshot in snapshot.children) {
                    // 핵심 수정: 현재 순회 중인 태그의 이름이 내가 삭제하려는 tag(A)와 같은지 확인
                    val currentTagName = tagSnapshot.child("tag").getValue(String::class.java)

                    if (currentTagName == tag) { // 클릭한 태그 이름과 일치할 때만 진입
                        val urlsSnapshot = tagSnapshot.child("url")
                        for (urlChild in urlsSnapshot.children) {
                            val urlValue =
                                urlChild.child("url").getValue(String::class.java) ?: continue

                            if (urlValue == urlTitle) {
                                urlChild.ref.removeValue().addOnSuccessListener {
                                    Log.d("삭제완료", "태그[$tag] 내에서 URL 삭제됨: $urlValue")
                                }
                            }
                        }
                    }
                }
            }

            /** onCancelled */
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. [url] 노드에서 해당 URL을 찾고 그 안의 태그 리스트 중 해당 태그 삭제
        userTagRef.orderByChild("url").equalTo(urlTitle)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                /** onDataChange */
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val urlKey = child.key ?: continue

                        // 해당 URL 내의 tags 리스트 중 이름이 tag(A)인 것만 삭제
                        child.child("tags").ref.orderByChild("tag").equalTo(tag)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                /** onDataChange */
                                override fun onDataChange(tagInUrlSnapshot: DataSnapshot) {
                                    for (data in tagInUrlSnapshot.children) {
                                        data.ref.removeValue().addOnSuccessListener {
                                            Log.d("삭제완료", "URL 내의 태그 리스트에서 [$tag] 삭제됨")
                                        }
                                    }
                                }

                                /** onCancelled */
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                /** onCancelled */
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /** insert ?? */
    fun insert(urlEntity: UrlEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insert(urlEntity)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 삽입 처리", e.toString())
            }
        }
    }

    /** 공유로 받은 링크를 게스트용 Room DB에 저장 */
    fun insertGuestUrl(url: Url) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (urlDao.getUrlIsExist(url.url) == null) {
                    urlDao.insert(
                        UrlEntity(
                            urlLink = url.url,
                            imageKey = url.imageKey ?: "",
                            favorite = url.favorite,
                            hidden = url.hidden,
                            timeStamp = url.timeStamp,
                            urlName = url.urlName,
                            urlMemo = url.urlMemo,
                            tag = null
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("UrlRepository", "Failed to insert guest shared URL", e)
            }
        }
    }

    /** 공유로 받은 링크를 로그인 사용자용 Room DB에 저장 */
    fun insertUserUrl(url: Url, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (urlDao.getBackupUrlIsExist(url.url) == null) {
                    urlDao.insertBackup(
                        UrlBackupEntity(
                            urlLink = url.url,
                            imageKey = url.imageKey ?: "",
                            imgUri = url.imgUri ?: "",
                            favorite = url.favorite,
                            hidden = url.hidden,
                            timeStamp = url.timeStamp,
                            urlName = url.urlName,
                            urlMemo = url.urlMemo,
                            tag = url.tag
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("UrlRepository", "Failed to insert user shared URL", e)
            }
        }
    }

    /** 로그인 사용자 공유 링크 전용 저장 로직: Room + Firebase 모두 저장 */
    fun saveSharedUrlForLoggedInUser(url: Url, userId: String) {
        val resolvedUserId = userId.ifBlank { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        if (resolvedUserId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localImageFile = resolveSharedImageForLocalSave(url, resolvedUserId)

                val uploadedDownloadUri = if (localImageFile != null && localImageFile.exists()) {
                    try {
                        uploadImageToStorage(resolvedUserId, localImageFile)?.toString()
                    } catch (e: Exception) {
                        Log.e("UrlRepository", "Pre-upload failed: ${e.message}")
                        null
                    }
                } else {
                    null
                }

                val uploadedStorageKey = localImageFile?.nameWithoutExtension?.takeIf { it.isNotBlank() }
                val finalImgUri = uploadedDownloadUri ?: ""
                val uploadedImagePath = extractStoragePathFromFirebaseUrl(finalImgUri)
                val finalImageKey = if (uploadedDownloadUri != null) {
                    uploadedImagePath
                        ?.substringAfterLast('/')
                        ?.substringBeforeLast('.', "")
                        ?.takeIf { it.isNotBlank() }
                        ?: uploadedStorageKey
                        ?: url.imageKey.ifBlank {
                        localImageFile?.nameWithoutExtension ?: java.util.UUID.randomUUID().toString()
                    }
                } else {
                    url.imageKey.ifBlank { "" }
                }

                Log.d("SHARE_THUMBNAIL_DEBUG", "[RECEIVER_SAVE] originalImageKey=${url.imageKey}, finalImageKey=$finalImageKey, uploadedDownloadUri=$uploadedDownloadUri, localImageFile=${localImageFile?.absolutePath ?: ""}")

                val resolvedSenderUid = url.senderUid?.takeIf { it.isNotBlank() }
                    ?: url.imagePath?.split('/')?.filter { it.isNotBlank() }?.getOrNull(1)
                    ?: ""
                val finalOwnerUid = if (uploadedDownloadUri != null) resolvedUserId else resolvedSenderUid
                val resolvedImagePath = when {
                    !uploadedImagePath.isNullOrBlank() -> uploadedImagePath
                    !url.imagePath.isNullOrBlank() -> url.imagePath
                    finalOwnerUid.isNotBlank() && finalImageKey.isNotBlank() -> "images/$finalOwnerUid/${finalImageKey}.png"
                    else -> ""
                }

                val finalUrl = url.copy(
                    imageKey = finalImageKey,
                    imgUri = finalImgUri,
                    timeStamp = System.currentTimeMillis(),
                    senderUid = finalOwnerUid.takeIf { it.isNotBlank() } ?: url.senderUid,
                    imagePath = resolvedImagePath.takeIf { it.isNotBlank() } ?: url.imagePath
                )

                Log.d("SHARE_THUMBNAIL_RESULT", "newImageKey=${finalImageKey}, newImageUri=${uploadedDownloadUri ?: finalImgUri}, finalSuccess=${uploadedDownloadUri != null}")

                if (urlDao.getBackupUrlIsExist(finalUrl.url) == null) {
                    urlDao.insertBackup(
                        UrlBackupEntity(
                            urlLink = finalUrl.url,
                            imageKey = finalUrl.imageKey ?: "",
                            imgUri = finalImgUri,
                            favorite = finalUrl.favorite,
                            hidden = finalUrl.hidden,
                            timeStamp = finalUrl.timeStamp,
                            urlName = finalUrl.urlName,
                            urlMemo = finalUrl.urlMemo,
                            tag = finalUrl.tag
                        )
                    )
                    Log.d("UrlRepository", "Saved shared URL to Room for user=$resolvedUserId : ${finalUrl.url} | imgUri=$finalImgUri")
                }

                val firebaseUrl = Url(
                    url = finalUrl.url,
                    urlName = finalUrl.urlName,
                    urlMemo = finalUrl.urlMemo,
                    imageKey = finalUrl.imageKey ?: "",
                    imgUri = uploadedDownloadUri ?: finalImgUri,
                    favorite = finalUrl.favorite,
                    hidden = finalUrl.hidden,
                    timeStamp = finalUrl.timeStamp,
                    senderUid = finalUrl.senderUid ?: url.senderUid,
                    imagePath = finalUrl.imagePath ?: url.imagePath
                )

                val userUrlRef = FirebaseDatabase.getInstance().reference
                    .child("User")
                    .child(resolvedUserId)
                    .child("url")

                userUrlRef.orderByChild("url").equalTo(finalUrl.url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                userUrlRef.child("img${firebaseUrl.timeStamp}")
                                    .setValue(firebaseUrl)
                                    .addOnSuccessListener {
                                        Log.d("UrlRepository", "Saved shared URL to Firebase for user=$resolvedUserId : ${finalUrl.url}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("UrlRepository", "Firebase save failed for user=$resolvedUserId : ${e.message}")
                                    }
                            } else {
                                Log.d("UrlRepository", "Shared URL already exists in Firebase for user=$resolvedUserId : ${finalUrl.url}")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("UrlRepository", "Firebase query cancelled for user=$resolvedUserId : ${error.message}")
                        }
                    })

            } catch (e: Exception) {
                Log.e("UrlRepository", "Failed to save shared URL for logged-in user", e)
            }
        }
    }

    /** hasGuestUrl */
    suspend fun hasGuestUrl(url: String): Boolean {
        return urlDao.getUrlIsExist(url) != null
    }

    /** hasBackupUrl */
    suspend fun hasBackupUrl(url: String): Boolean {
        return urlDao.getBackupUrlIsExist(url) != null
    }

    /** insertBackup */
    fun insertBackup(urlBackupEntity: UrlBackupEntity, file: File, tag: String, userId: String) {

        val tagData = listOf(
            TagBackupEntity(
                tag = tag,
                count = "1",
                timeStamp = System.currentTimeMillis().toString(),
                urlList = listOf(urlBackupEntity.urlLink),
                firebaseTagId = "tag${urlBackupEntity.timeStamp}"
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insertBackup(urlBackupEntity)
                insertTagBackup(tagData, tag)

                Log.d("UrlRepository", "Attempting upload: file=${file.absolutePath}, exists=${file.exists()}, length=${file.length()}")
                val downloadUri = uploadImageToStorage(userId, file)
                if (downloadUri != null) {
                    Log.d("UrlRepository", "Upload succeeded, uri=$downloadUri")
                    urlDao.insertImgUri(downloadUri.toString(), urlBackupEntity.urlLink)
                } else {
                    Log.e("UrlRepository", "Upload returned null for user=$userId, file=${file.absolutePath}")
                }

                Log.e("유저 아이디 확인", userId)
                val userRef: DatabaseReference = database.child("User").child(userId).child("url")
                val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")
                val urlLink = urlBackupEntity.urlLink

                val url = Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    timeStamp = urlBackupEntity.timeStamp,
                    urlName = urlBackupEntity.urlName,
                )

                val userTags = UserTags(
                    tag = tag, timeStamp = urlBackupEntity.timeStamp
                )

                updateTagInTagFirebase(
                    tag = tag,
                    tagRef = tagRef,
                    urlLink = url.url,
                    preferredTagId = "tag${urlBackupEntity.timeStamp}"
                )

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
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

            } catch (e: java.lang.Exception) {
                Log.e("데이터 삽입 처리", e.toString())
            }
        }
    }

    /** insertTagBackup */
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

    /** 태그 중복 저장 가능 함수 **/
    fun insertBackupMultipleTags(
        urlBackupEntity: UrlBackupEntity,
        file: File,
        tags: List<UserTags>,
        userId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.insertBackup(urlBackupEntity)

                val tagData = tags.map { userTag ->
                    TagBackupEntity(
                        tag = userTag.tag ?: "",
                        count = "1",
                        timeStamp = userTag.timeStamp.toString(),
                        urlList = listOf(urlBackupEntity.urlLink),
                        firebaseTagId = "tag${userTag.timeStamp}"
                    )
                }

                insertTagBackupMultiple(tagData)

                Log.d("UrlRepository", "Attempting upload (multiple tags): file=${file.absolutePath}, exists=${file.exists()}, length=${file.length()}")
                val downloadUri = uploadImageToStorage(userId, file)
                if (downloadUri != null) {
                    Log.d("UrlRepository", "Upload (multiple tags) succeeded, uri=$downloadUri")
                    urlDao.insertImgUri(downloadUri.toString(), urlBackupEntity.urlLink)
                } else {
                    Log.e("UrlRepository", "Upload (multiple tags) returned null for user=$userId, file=${file.absolutePath}")
                }

                val userRef: DatabaseReference = database.child("User").child(userId).child("url")
                val tagRef: DatabaseReference = database.child("User").child(userId).child("Tag")
                val urlLink = urlBackupEntity.urlLink

                val url = Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    timeStamp = urlBackupEntity.timeStamp,
                    urlName = urlBackupEntity.urlName,
                    urlMemo = urlBackupEntity.urlMemo
                )

                userRef.orderByChild("url").equalTo(urlLink)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                userRef.child("img" + url.timeStamp).setValue(url)
                                tags.forEach { tagItem ->
                                    val userTag = UserTags(tag = tagItem.tag, timeStamp = tagItem.timeStamp)
                                    userRef.child("img" + url.timeStamp)
                                        .child("tags")
                                        .child("tag" + tagItem.timeStamp)
                                        .setValue(userTag)

                                    updateTagInTagFirebase(
                                        tag = userTag.tag ?: "",
                                        tagRef = tagRef,
                                        urlLink = url.url,
                                        preferredTagId = "tag${userTag.timeStamp}"
                                    )
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

            } catch (e: Exception) {
                Log.e("데이터 삽입 처리", e.toString())
            }
        }
    }

    private suspend fun resolveSharedImageForLocalSave(url: Url, receiverUid: String): File? {
        val app = getApplication<Application>()
        val senderStorageFile = resolveSharedImageForReceiverStorage(url, receiverUid)
        if (senderStorageFile != null) {
            return senderStorageFile
        }

        if (url.senderUid.isNullOrBlank() || url.imagePath.isNullOrBlank()) {
            Log.e("SHARE_THUMBNAIL_RESULT", "newImageKey=, newImageUri=, finalSuccess=false")
            Log.e("UrlRepository", "No valid sender metadata for shared thumbnail copy; refusing fallback image. senderUid=${url.senderUid ?: ""}, imagePath=${url.imagePath ?: ""}, receiverUid=$receiverUid")
            return null
        }

        val candidateUri = url.imgUri
        if (candidateUri.isNotBlank()) {
            val fileCandidate = File(candidateUri)
            if (fileCandidate.exists() && fileCandidate.isFile) return fileCandidate

            val parsed = runCatching { Uri.parse(candidateUri) }.getOrNull()
            if (parsed != null && (parsed.scheme == "content" || parsed.scheme == "file")) {
                return try {
                    val inputStream = app.contentResolver.openInputStream(parsed) ?: return null
                    val output = File(app.filesDir, "shared_${System.currentTimeMillis()}.png")
                    inputStream.use { inStream ->
                        output.outputStream().use { out ->
                            inStream.copyTo(out)
                        }
                    }
                    output
                } catch (e: Exception) {
                    Log.e("UrlRepository", "Failed to copy shared image from uri=${candidateUri}", e)
                    null
                }
            }

            if (candidateUri.startsWith("http://") || candidateUri.startsWith("https://")) {
                return try {
                    val stream = java.net.URL(candidateUri).openStream()
                    val output = File(app.filesDir, "shared_${System.currentTimeMillis()}.png")
                    stream.use { inStream ->
                        output.outputStream().use { out ->
                            inStream.copyTo(out)
                        }
                    }
                    output
                } catch (e: Exception) {
                    Log.e("UrlRepository", "Failed to download shared image from url=${candidateUri}", e)
                    null
                }
            }
        }

        return null
    }

    private suspend fun uploadImageToStorage(userId: String, file: File): Uri? {
        val resolvedUserId = userId.ifBlank { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        if (resolvedUserId.isBlank()) {
            Log.e("FirebaseStorage", "Storage upload skipped: empty userId and no Firebase auth user")
            return null
        }

        fun computeSha256(f: File): String {
            return try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                f.inputStream().use { inp ->
                    var read: Int
                    while (inp.read(buffer).also { read = it } > 0) {
                        md.update(buffer, 0, read)
                    }
                }
                md.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e("SHARE_HASH", "Failed to compute hash: ${e.message}")
                ""
            }
        }

        val fileSize = file.length()
        val fileHash = computeSha256(file)

        val storageRef = FirebaseStorage.getInstance().reference.child("images/${resolvedUserId}/")
        val safeName = file.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val fileRef = storageRef.child(safeName)
        val receiverStoragePath = fileRef.path
        val fileUri = Uri.fromFile(file)

        Log.d("SHARE_THUMBNAIL_UPLOAD", "receiverStoragePath=${receiverStoragePath}, uploadStart=true, size=${fileSize}, hash=${fileHash}")

        var attempt = 0
        val maxAttempts = 3
        var lastException: Exception? = null

        while (attempt < maxAttempts) {
            attempt++
            try {
                val uploadTask = fileRef.putFile(fileUri)
                uploadTask.await()

                // attach custom metadata with hash for verification
                try {
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                        .setCustomMetadata("sha256", fileHash)
                        .build()
                    fileRef.updateMetadata(metadata).await()
                } catch (metaEx: Exception) {
                    Log.w("SHARE_THUMBNAIL_UPLOAD", "Failed to set metadata: ${metaEx.message}")
                }

                val meta = fileRef.metadata.await()
                val remoteSize = meta.sizeBytes
                val remoteHash = meta.getCustomMetadata("sha256")

                if (remoteSize == fileSize && (remoteHash == null || remoteHash == fileHash || remoteHash.isBlank())) {
                    val downloadUrl = fileRef.downloadUrl.await()
                    Log.d("SHARE_THUMBNAIL_UPLOAD", "uploadSuccess=true receiverStoragePath=${receiverStoragePath} downloadUrl=$downloadUrl")
                    return downloadUrl
                } else {
                    Log.w("SHARE_THUMBNAIL_UPLOAD", "Verification failed size: local=$fileSize remote=$remoteSize hashLocal=$fileHash hashRemote=$remoteHash; attempt=$attempt")
                    // try to delete the potentially corrupted remote file before retry
                    try {
                        fileRef.delete().await()
                    } catch (delEx: Exception) {
                        Log.w("SHARE_THUMBNAIL_UPLOAD", "Failed to delete failed upload: ${delEx.message}")
                    }
                }

            } catch (e: Exception) {
                lastException = e
                Log.w("SHARE_THUMBNAIL_UPLOAD", "Upload attempt $attempt failed: ${e.message}")
                try {
                    // small backoff
                    kotlinx.coroutines.delay((500L * attempt))
                } catch (_: InterruptedException) {}
            }
        }

        lastException?.let { e ->
            val errorCode = if (e is com.google.firebase.storage.StorageException) e.errorCode else "UNKNOWN"
            val errorMessage = e.message ?: "no message"
            Log.e("SHARE_THUMBNAIL_UPLOAD", "receiverStoragePath=${"images/" + userId + "/" + file.name}, uploadStart=true, uploadSuccess=false, errorCode=${errorCode}, errorMessage=${errorMessage}", e)
        }

        Log.e("FirebaseStorage", "회원 이미지 업로드 실패 after retries: ${lastException?.message}")
        return null
    }

    /** insertTagBackupMultiple */
    private fun insertTagBackupMultiple(tagBackupEntityList: List<TagBackupEntity>) {
        viewModelScope.launch(Dispatchers.IO) {

            if (tagBackupEntityList.isNotEmpty()) {

                // 1️⃣ 저장하려는 태그 리스트 가져오기
                val urlTagList = tagBackupEntityList.map { it.tag }

                // 2️⃣ 이미 존재하는 태그 가져오기 (DB에서 조회)
                val existingTags = urlDao.getTagBackupByTags(urlTagList).associateBy { it.tag }

                // 3️⃣ 새로운 태그 & 업데이트할 태그 분리
                val newTags = mutableListOf<TagBackupEntity>()
                val tagsToUpdate = mutableListOf<TagBackupEntity>()

                for (tagEntity in tagBackupEntityList) {
                    val existingTag = existingTags[tagEntity.tag]

                    if (existingTag != null) {
                        // 이미 존재하는 태그 → urlList 업데이트
                        val updatedUrls =
                            (existingTag.urlList.orEmpty() + tagEntity.urlList.orEmpty()).distinct()
                        val updatedTagEntity = existingTag.copy(urlList = updatedUrls)
                        tagsToUpdate.add(updatedTagEntity)
                    } else {
                        // 없는 태그 → 새로 추가
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

    private suspend fun resolveSharedImageForReceiverStorage(url: Url, receiverUid: String): File? {
        val app = getApplication<Application>()
        val senderUid = url.senderUid?.takeIf { it.isNotBlank() }
        val imagePath = url.imagePath?.takeIf { it.isNotBlank() }
        Log.d("SHARE_THUMBNAIL_DEBUG", "senderUid=${senderUid ?: ""}, imagePath=${imagePath ?: ""}, imageKey=${url.imageKey}, receiverUid=${receiverUid}")

        if (senderUid.isNullOrBlank() || imagePath.isNullOrBlank()) {
            Log.e("SHARE_THUMBNAIL_DOWNLOAD", "storagePath=EMPTY, downloadStart=true, downloadSuccess=false, errorCode=INVALID_SHARE_METADATA, errorMessage=senderUid or imagePath is missing")
            return null
        }

        val fileRef = FirebaseStorage.getInstance().reference.child(imagePath)
        val storagePath = fileRef.path
        Log.d("SHARE_THUMBNAIL_DOWNLOAD", "storagePath=${storagePath}, downloadStart=true")

        return try {
            val bytes = fileRef.getBytes(10L * 1024 * 1024).await()
            val targetFile = File(app.filesDir, "shared_${System.currentTimeMillis()}_${url.imageKey.ifBlank { "thumbnail" }}.jpg")
            targetFile.writeBytes(bytes)
            Log.d("SHARE_THUMBNAIL_LOCAL", "localFilePath=${targetFile.absolutePath}, fileExists=${targetFile.exists()}, fileSize=${targetFile.length()}")
            targetFile
        } catch (e: Exception) {
            val errorCode = if (e is com.google.firebase.storage.StorageException) e.errorCode else "UNKNOWN"
            val errorMessage = e.message ?: "no message"
            Log.e("SHARE_THUMBNAIL_DOWNLOAD", "storagePath=${storagePath}, downloadStart=true, downloadSuccess=false, errorCode=${errorCode}, errorMessage=${errorMessage}", e)
            Log.e("UrlRepository", "Failed to copy shared image from sender storage path=${storagePath} senderUid=${senderUid} receiverUid=${receiverUid}", e)
            null
        }
    }

    /** updateTagInTagFirebase */
    private fun updateTagInTagFirebase(
        tag: String,
        tagRef: DatabaseReference,
        urlLink: String,
        preferredTagId: String? = null
    ) {

        if (tag.isNotBlank()) {

            tagRef.orderByChild("tag").equalTo(tag)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    /** onDataChange */
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val urlInTag = UrlInTag(url = urlLink)

                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val key = child.key
                                val timeStamp = System.currentTimeMillis()
                                if (!key.isNullOrBlank()) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        runCatching { urlDao.updateTagFirebaseId(tag, key) }
                                    }
                                }

                                val tagCountRef = tagRef.child(key!!).child("count")
                                tagCountRef.runTransaction(object : Transaction.Handler {
                                    /** doTransaction */
                                    override fun doTransaction(data: MutableData): Transaction.Result {
                                        var tagCount = data.getValue(Int::class.java) ?: 0
                                        tagCount++
                                        data.value = tagCount
                                        return Transaction.success(data)
                                    }

                                    /** onComplete */
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
                                        /** onDataChange */
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

                                        /** onCancelled */
                                        override fun onCancelled(error: DatabaseError) {

                                        }
                                    })


                                Log.e("데이터 중복 여부", "중복되는 태그가 존재함")
                            }

                        } else {
                            val timeStamp = System.currentTimeMillis()
                            val tagId = preferredTagId
                                ?.takeIf { it.isNotBlank() && it.startsWith("tag") }
                                ?: "tag$timeStamp"

                            val tagInfo = Tag(tag = tag, count = "1", timeStamp = timeStamp.toString(), id = tagId)

                            tagRef.child(tagId).setValue(tagInfo)
                            tagRef.child(tagId).child("id").setValue(tagId)
                            tagRef.child(tagId).child("url").child("url$timeStamp")
                                .setValue(urlInTag)
                            viewModelScope.launch(Dispatchers.IO) {
                                runCatching { urlDao.updateTagFirebaseId(tag, tagId) }
                            }

                            /**
                            tagRef.child("tag" + url.timeStamp).child("tag").setValue(tag)

                            tagRef.child("tag" + url.timeStamp).child("timeStamp")
                            .setValue(timeStamp)

                            tagRef.child("tag" + url.timeStamp).child("count").setValue(1)
                             **/

                        }
                    }

                    /** onCancelled */
                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }


    }

    /** updateGuestUrlName */
    fun updateGuestUrlName(url: String, urlName: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateGuestUrlName(url, urlName)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }

    }

    /** updateGuestUrlMemo */
    fun updateGuestUrlMemo(url: String, urlMemo: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateGuestUrlMemo(url, urlMemo)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }

    }

    /** updateGuestUrlLink */
    fun updateGuestUrlLink(oldUrl: String, newUrl: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateGuestUrlLink(oldUrl, newUrl)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }

    }

    /** updateUrlInfo */
    fun updateUrlInfo(url: String, urlName: String, urlMemo: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                urlDao.updateUrlInfo(url, urlName, urlMemo)

                databaseReference =
                    FirebaseDatabase.getInstance().reference.child("User").child(userId)
                        .child("url")

                databaseReference.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        /** onDataChange */
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

                        /** onCancelled */
                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

            } catch (e: java.lang.Exception) {

            }

        }

    }

    /** update */
    fun update(urlEntity: UrlEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.update(urlEntity.urlLink, urlEntity.imageKey)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    /** updateBackup */
    fun updateBackup(urlBackupEntity: UrlBackupEntity, file: File, userId: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val databaseReference =
                    FirebaseDatabase.getInstance().reference.child("User").child(userId)
                        .child("url")

                val snapshot = databaseReference.orderByChild("url").equalTo(urlBackupEntity.urlLink)
                    .get()
                    .await()
                val child = snapshot.children.firstOrNull() ?: run {
                    Log.e("데이터 없음", "해당 URL을 가진 데이터가 없습니다.")
                    return@launch
                }

                val oldImageKey = child.child("imageKey").getValue(String::class.java)
                val oldImagePath = child.child("imagePath").getValue(String::class.java)
                val oldImgUri = child.child("imgUri").getValue(String::class.java)

                val uploadedUri = uploadImageToStorage(userId, file)?.toString()
                if (uploadedUri.isNullOrBlank()) {
                    Log.e("Storage Upload", "파일 업로드 실패: ${file.name}")
                    return@launch
                }

                val uploadedImagePath = extractStoragePathFromFirebaseUrl(uploadedUri).orEmpty()
                val uploadedImageKey = uploadedImagePath.substringAfterLast('/')
                    .substringBeforeLast('.', "")
                    .ifBlank {
                        file.nameWithoutExtension.ifBlank { urlBackupEntity.imageKey }
                    }

                val updates = mutableMapOf<String, Any>(
                    "imageKey" to uploadedImageKey,
                    "imgUri" to uploadedUri,
                    "senderUid" to userId
                )
                if (uploadedImagePath.isNotBlank()) {
                    updates["imagePath"] = uploadedImagePath
                }

                child.ref.updateChildren(updates).await()

                urlDao.updateBackup(urlBackupEntity.urlLink, uploadedImageKey)
                urlDao.insertImgUri(uploadedUri, urlBackupEntity.urlLink)

                val normalizedOldImagePath = normalizeStoragePath(oldImagePath)
                val oldPathFromImgUri = extractStoragePathFromFirebaseUrl(oldImgUri.orEmpty())
                val oldStoragePath = oldPathFromImgUri ?: normalizedOldImagePath
                val shouldDeleteOld =
                    oldStoragePath.isNullOrBlank() || oldStoragePath != uploadedImagePath

                if (shouldDeleteOld) {
                    deleteStorageImageByMetadata(
                        userId = userId,
                        imageKey = oldImageKey,
                        imagePath = oldImagePath,
                        imgUri = oldImgUri
                    )
                }

                Log.d(
                    "Storage Upload",
                    "파일 업로드/교체 성공: ${file.name}, oldImageKey=${oldImageKey ?: ""}, newImageKey=$uploadedImageKey"
                )
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

    /** urldetail 액티비티에서 이름 수정 함수 **/
    fun updateUrlName(url: String, urlName: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (userId.isEmpty()) return@launch

                // 1. 로컬 DB 업데이트 (기존 코드 유지)
                urlDao.updateUrlName(url, urlName)

                // 2. Firebase 업데이트를 위한 참조 설정
                val userUrlRef = FirebaseDatabase.getInstance().reference
                    .child("User").child(userId).child("url")

                // 3. url 필드가 매개변수 url과 일치하는 노드 찾기
                userUrlRef.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        /** onDataChange */
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (childSnapshot in snapshot.children) {
                                    // 일치하는 노드의 urlName 필드만 업데이트
                                    childSnapshot.ref.child("urlName").setValue(urlName)
                                        .addOnSuccessListener {
                                            Log.d("데이터 업데이트", "Firebase 업데이트 성공")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("데이터 업데이트", "Firebase 업데이트 실패: ${e.message}")
                                        }
                                }

                            } else {
                                Log.d("데이터 업데이트", "일치하는 URL을 찾을 수 없습니다.")
                            }
                        }

                        /** onCancelled */
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("데이터 업데이트", "쿼리 취소됨: ${error.message}")
                        }
                    })

            } catch (e: Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    /** urldetail 액티비티에서 메모 수정 함수 **/
    fun updateUrlMemo(url: String, urlMemo: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (userId.isEmpty()) return@launch

                // 1. 로컬 DB 업데이트
                urlDao.updateUrlMemo(url, urlMemo)

                // 2. Firebase 참조
                val userUrlRef = FirebaseDatabase.getInstance().reference
                    .child("User").child(userId).child("url")

                // 3. url이 같은 노드 찾기
                userUrlRef.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {

                        /** onDataChange */
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (childSnapshot in snapshot.children) {

                                    // urlMemo 필드만 업데이트
                                    childSnapshot.ref.child("urlMemo").setValue(urlMemo)
                                        .addOnSuccessListener {
                                            Log.d("메모 업데이트", "Firebase 업데이트 성공")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("메모 업데이트", "Firebase 업데이트 실패: ${e.message}")
                                        }
                                }

                            } else {
                                Log.d("메모 업데이트", "일치하는 URL을 찾을 수 없습니다.")
                            }
                        }

                        /** onCancelled */
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("메모 업데이트", "쿼리 취소됨: ${error.message}")
                        }
                    })

            } catch (e: Exception) {
                Log.e("메모 업데이트 처리", e.toString())
            }
        }
    }

    /** urldetail 액티비티에서 링크 수정 함수 **/
    fun updateUrlLink(oldUrl: String, newUrl: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (userId.isEmpty()) return@launch

                urlDao.updateUrlLink(oldUrl, newUrl)
                updateTagBackupUrlLink(oldUrl, newUrl)

                val userUrlRef = FirebaseDatabase.getInstance().reference
                    .child("User").child(userId).child("url")

                userUrlRef.orderByChild("url").equalTo(oldUrl)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (childSnapshot in snapshot.children) {
                                    childSnapshot.ref.child("url").setValue(newUrl)
                                        .addOnSuccessListener {
                                            Log.d("링크 업데이트", "Firebase URL 업데이트 성공")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("링크 업데이트", "Firebase URL 업데이트 실패: ${e.message}")
                                        }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("링크 업데이트", "쿼리 취소됨: ${error.message}")
                        }
                    })

                updateTagUrlInFirebase(userId, oldUrl, newUrl)
            } catch (e: Exception) {
                Log.e("링크 업데이트 처리", e.toString())
            }
        }
    }

    private suspend fun updateTagBackupUrlLink(oldUrl: String, newUrl: String) {
        val tagBackups = urlDao.getAllTagBackups()
        val tagsToUpdate = tagBackups.mapNotNull { tagEntity ->
            val currentUrlList = tagEntity.urlList ?: return@mapNotNull null
            if (!currentUrlList.contains(oldUrl)) {
                return@mapNotNull null
            }

            val replacedUrlList = currentUrlList.map { urlValue ->
                if (urlValue == oldUrl) newUrl else urlValue
            }
            tagEntity.copy(urlList = replacedUrlList)
        }

        if (tagsToUpdate.isNotEmpty()) {
            urlDao.updateUrlInTags(tagsToUpdate)
        }
    }

    private fun updateTagUrlInFirebase(userId: String, oldUrl: String, newUrl: String) {
        val tagRef = FirebaseDatabase.getInstance().reference
            .child("User").child(userId).child("Tag")

        tagRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tagSnapshot in snapshot.children) {
                    val urlSnapshot = tagSnapshot.child("url")
                    for (urlChild in urlSnapshot.children) {
                        val currentUrl = urlChild.child("url").getValue(String::class.java) ?: continue
                        if (currentUrl == oldUrl) {
                            urlChild.ref.child("url").setValue(newUrl)
                                .addOnFailureListener { e ->
                                    Log.e("링크 업데이트", "Tag URL 업데이트 실패: ${e.message}")
                                }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("링크 업데이트", "Tag 쿼리 취소됨: ${error.message}")
            }
        })
    }
    /** updateFavorite */
    suspend fun updateFavorite(url: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.updateFavorite(url, isFavorite)
            } catch (e: java.lang.Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    /** updateUserFavorite */
    suspend fun updateUserFavorite(url: String, isFavorite: Boolean, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Room 먼저 업데이트
                urlDao.updateUserFavorite(url, isFavorite)

                // Firebase Query
                val query = FirebaseDatabase.getInstance().reference
                    .child("User")
                    .child(userId)
                    .child("url")
                    .orderByChild("url")
                    .equalTo(url)

                // 1회 조회를 suspend로 대기
                val snapshot = query.get().await()

                if (!snapshot.exists()) {
                    Log.e("데이터 없음", "해당 URL을 가진 데이터가 없습니다.")
                    return@withContext
                }

                // 즐겨찾기 값 업데이트
                snapshot.children.forEach { child ->
                    child.ref.child("favorite").setValue(isFavorite).await()
                }

                Log.e("업데이트 성공", "Firebase favorite 업데이트 완료")
            } catch (e: Exception) {
                Log.e("데이터 업데이트 처리", e.toString())
            }
        }
    }

    /** deleteGuestData */
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

    /** deleteUserTag */
    fun deleteUserTag(tag: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                urlDao.deleteTag(tag)
                val rootRef = FirebaseDatabase.getInstance().reference.child("User").child(userId)
                val tagRef = rootRef.child("Tag")
                val urlMainRef = rootRef.child("url")

                tagRef.orderByChild("tag").equalTo(tag)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        /** onDataChange */
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val tagIdsToRemove = mutableListOf<String>()

                            for (tagSnapshot in snapshot.children) {
                                val tagKey = tagSnapshot.key
                                if (tagKey != null) tagIdsToRemove.add(tagKey)

                                val linkedUrlsNode = tagSnapshot.child("url")

                                linkedUrlsNode.children.forEach { urlChild ->

                                    val realUrlString =
                                        urlChild.child("url").getValue(String::class.java)

                                    if (realUrlString != null) {
                                        deleteUserUrlTag(tag, realUrlString, userId)

                                        urlMainRef.orderByChild("url").equalTo(realUrlString)
                                            .addListenerForSingleValueEvent(object :
                                                ValueEventListener {
                                                /** onDataChange */
                                                override fun onDataChange(imgSnapshot: DataSnapshot) {
                                                    imgSnapshot.children.forEach { matchingImg ->

                                                        val tagsNode = matchingImg.child("tags")
                                                        tagsNode.children.forEach { bTag ->
                                                            if (bTag.child("tag").value == tag) {
                                                                bTag.ref.removeValue()
                                                                    .addOnSuccessListener {
                                                                        Log.i(
                                                                            "deleteTag",
                                                                            "B태그 삭제 완료: ${matchingImg.key}"
                                                                        )
                                                                    }
                                                            }
                                                        }
                                                    }
                                                }

                                                /** onCancelled */
                                                override fun onCancelled(error: DatabaseError) {}
                                            })
                                    }
                                }

                                tagSnapshot.ref.removeValue().addOnSuccessListener {
                                    Log.i("deleteTag", "A태그(수탉) 삭제 성공")
                                }
                            }

                            // ➊ Firebase: tagOrder에서 삭제된 tagId 제거
                            if (tagIdsToRemove.isNotEmpty()) {
                                val tagOrderRef = rootRef.child("tagOrder")
                                tagOrderRef.runTransaction(object : Transaction.Handler {
                                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                                        val existingValues = currentData.children.mapNotNull { it.getValue(String::class.java) }
                                        val updated = existingValues.filter { it !in tagIdsToRemove }
                                        currentData.value = updated
                                        return Transaction.success(currentData)
                                    }

                                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                                        if (error != null) {
                                            Log.e("deleteTagOrder", "Failed to update tagOrder: ${error.message}")
                                        } else if (committed) {
                                            Log.i("deleteTagOrder", "tagOrder updated, removed: ${tagIdsToRemove}")
                                        }
                                    }
                                })

                                // ➋ Local Room: remove tagIds from stored tagOrder lists
                                viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        val current = urlDao.getAllTagBackups()
                                        if (current.isNotEmpty()) {
                                            val updated = current.map { entity ->
                                                val newOrder = entity.tagOrder?.filter { it !in tagIdsToRemove }
                                                entity.copy(tagOrder = newOrder)
                                            }
                                            urlDao.updateTagOrder(updated)
                                            Log.i("deleteTagOrderLocal", "Local tagOrder lists updated, removed: ${tagIdsToRemove}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("deleteTagOrderLocal", "Failed to update local tagOrder: ${e.message}")
                                    }
                                }
                            }
                        }

                        /** onCancelled */
                        override fun onCancelled(error: DatabaseError) {}
                    })
            } catch (e: Exception) {
                Log.e("deleteTag", e.toString())
            }
        }
    }

    /** deleteUserData */
    fun deleteUserData(url: String, imageKey: String, userId: String) {
        val databaseRef = FirebaseDatabase.getInstance().reference
        val urlRef = databaseRef.child("User").child(userId).child("url")
        val tagRef = databaseRef.child("User").child(userId).child("Tag")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1️⃣ 로컬 DB에서 URL 삭제
                urlDao.deleteUserUrl(url)

                // 2️⃣ Firebase URL 노드에서 삭제
                withContext(Dispatchers.Main) {
                    urlRef.orderByChild("url").equalTo(url)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            /** onDataChange */
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (child in snapshot.children) {
                                        val childImageKey = child.child("imageKey").getValue(String::class.java)
                                        val childImagePath = child.child("imagePath").getValue(String::class.java)
                                        val childImgUri = child.child("imgUri").getValue(String::class.java)

                                        child.ref.removeValue().addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Log.i("deleteKeyword", "URL 삭제 완료: $url")

                                                // 3️⃣ URL 삭제 후 전체 Tag에서도 해당 URL 제거
                                                deleteUrlFromTags(tagRef, url)

                                                // 4️⃣ 스토리지 이미지 삭제 (imagePath/imgUri 우선, imageKey는 fallback)
                                                viewModelScope.launch(Dispatchers.IO) {
                                                    deleteStorageImageByMetadata(
                                                        userId = userId,
                                                        imageKey = childImageKey ?: imageKey,
                                                        imagePath = childImagePath,
                                                        imgUri = childImgUri
                                                    )
                                                }
                                            } else {
                                                Log.e("deleteKeyword fail", "URL 삭제 실패")
                                            }
                                        }
                                    }
                                } else {
                                    Log.e("deleteKeyword", "URL Firebase에 존재하지 않음: $url")
                                }
                            }

                            /** onCancelled */
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Firebase Error", "Failed to read data: ${error.message}")
                            }
                        })
                }

            } catch (e: Exception) {
                Log.e("삭제 처리 예외", e.toString())
            }
        }
    }

    /** 전체 Tag에서 해당 URL 제거 **/
    private fun deleteUrlFromTags(tagRef: DatabaseReference, urlTitle: String) {
        tagRef.addListenerForSingleValueEvent(object : ValueEventListener {
            /** onDataChange */
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tagSnapshot in snapshot.children) {
                    val urlsSnapshot = tagSnapshot.child("url")
                    if (!urlsSnapshot.exists()) continue

                    for (urlChild in urlsSnapshot.children) {
                        val urlValue = urlChild.child("url").getValue(String::class.java)
                        if (urlValue == urlTitle) {
                            // URL 값이 정확히 일치하면 삭제
                            urlChild.ref.removeValue()
                                .addOnSuccessListener {
                                    Log.d("삭제완료", "Tag[${tagSnapshot.child("tag").value}]에서 URL 삭제됨: $urlTitle")
                                }
                                .addOnFailureListener {
                                    Log.e("삭제실패", "Tag[${tagSnapshot.child("tag").value}] 삭제 실패: $urlTitle")
                                }
                        }
                    }
                }
            }

            /** onCancelled */
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private suspend fun deleteStorageImageByMetadata(
        userId: String,
        imageKey: String?,
        imagePath: String?,
        imgUri: String?
    ): Boolean {
        val candidates = buildStorageDeleteCandidates(userId, imageKey, imagePath, imgUri)
        if (candidates.isEmpty()) {
            Log.e(
                "스토리지 삭제",
                "실패: 삭제 후보 경로 없음 imageKey=${imageKey ?: ""} imagePath=${imagePath ?: ""} imgUri=${imgUri ?: ""}"
            )
            return false
        }

        val storage = FirebaseStorage.getInstance().reference
        var deleted = false
        var lastError: Exception? = null

        for (path in candidates) {
            try {
                storage.child(path).delete().await()
                Log.d("스토리지 삭제", "성공: $path")
                deleted = true
                break
            } catch (e: Exception) {
                lastError = e
                val isNotFound = (e as? com.google.firebase.storage.StorageException)
                    ?.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND
                if (!isNotFound) {
                    Log.e("스토리지 삭제", "실패: $path (${e.message})")
                }
            }
        }

        if (!deleted) {
            Log.e(
                "스토리지 삭제",
                "최종 실패: candidates=${candidates.joinToString()} error=${lastError?.message ?: "unknown"}"
            )
        }

        return deleted
    }

    private fun buildStorageDeleteCandidates(
        userId: String,
        imageKey: String?,
        imagePath: String?,
        imgUri: String?
    ): List<String> {
        val candidates = linkedSetOf<String>()

        val pathFromImgUri = extractStoragePathFromFirebaseUrl(imgUri.orEmpty())
        if (!pathFromImgUri.isNullOrBlank()) {
            candidates.add(pathFromImgUri)
        }

        val normalizedPath = normalizeStoragePath(imagePath)
        if (!normalizedPath.isNullOrBlank()) {
            val pathOwnerUid = parseOwnerUidFromStoragePath(normalizedPath)
            val canUseImagePath = pathOwnerUid.isNullOrBlank() || pathOwnerUid == userId || pathFromImgUri.isNullOrBlank()
            if (canUseImagePath) {
                candidates.add(normalizedPath)
            }
        }

        val normalizedImageKey = imageKey?.trim().orEmpty()
        if (normalizedImageKey.isNotBlank()) {
            if (normalizedImageKey.contains("/")) {
                candidates.add(normalizedImageKey.trimStart('/'))
            } else {
                candidates.add("images/$userId/$normalizedImageKey")
                if (!normalizedImageKey.contains(".")) {
                    candidates.add("images/$userId/$normalizedImageKey.png")
                    candidates.add("images/$userId/$normalizedImageKey.jpg")
                    candidates.add("images/$userId/$normalizedImageKey.jpeg")
                }
            }
        }

        return candidates.filter { it.isNotBlank() }
    }

    private fun normalizeStoragePath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            java.net.URLDecoder.decode(path.substringBefore("?"), "UTF-8").trim().trimStart('/')
        }.getOrNull() ?: path.substringBefore("?").trim().trimStart('/')
    }

    private fun parseOwnerUidFromStoragePath(path: String): String? {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        return if (segments[0] == "images") segments[1] else null
    }

    private fun extractStoragePathFromFirebaseUrl(rawUrl: String): String? {
        if (rawUrl.isBlank()) return null
        return try {
            val decodedUrl = java.net.URL(rawUrl)
            val path = decodedUrl.path
            val startIndex = path.indexOf("/o/")
            if (startIndex < 0) return null
            val encoded = path.substring(startIndex + 3).substringBefore("?")
            java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
        } catch (e: Exception) {
            Log.w("UrlRepository", "Failed to extract storage path from Firebase URL: ${e.message}")
            null
        }
    }

    /** deleteUserBackup */
    fun deleteUserBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUserBackup()
            } catch (e: java.lang.Exception) {
                Log.e("로그아웃 시, 백업 데이터 전체 삭제", e.toString())
            }
        }
    }

    /** deleteUserTagBackup */
    fun deleteUserTagBackup() {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                urlDao.deleteUserTagBackup()
            } catch (e: java.lang.Exception) {
                Log.e("로그아웃 시, 백업 데이터 전체 삭제", e.toString())
            }
        }

    }

    /** getGuestUrlFlow */
    fun getGuestUrlFlow(): Flow<List<UrlEntity>> = urlDao.getAllFlow()

    /** getUserUrlBackupFlow */
    fun getUserUrlBackupFlow(): Flow<List<UrlBackupEntity>> = urlDao.getAllBackupFlow()

    /** getUserTagBackupFlow */
    fun getUserTagBackupFlow(): Flow<List<TagBackupEntity>> = urlDao.getTagBackupFlow()

    /** getGuestUrl */
    fun getGuestUrl(): LiveData<List<UrlEntity>> = url

    /** getUserUrlBackup */
    fun getUserUrlBackup(): LiveData<List<UrlBackupEntity>> = urlBackup

    /** getUserTagBackup */
    fun getUserTagBackup(): LiveData<List<TagBackupEntity>> = tagBackup

    /** hasBackupData */
    suspend fun hasBackupData(): Boolean {
        return urlDao.hasBackupData() // suspend 함수 호출
    }

    /** hideUrl - Firebase와 Room에 hidden=true 설정 */
    fun hideUrl(url: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 로컬 DB 업데이트
                urlDao.updateUrlBackupHidden(url, true)

                // 2. Firebase 업데이트
                val userUrlRef = FirebaseDatabase.getInstance().reference
                    .child("User").child(userId).child("url")

                userUrlRef.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (childSnapshot in snapshot.children) {
                                    childSnapshot.ref.child("hidden").setValue(true)
                                        .addOnSuccessListener {
                                            Log.d("URL 숨기기", "Firebase 업데이트 성공: $url")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("URL 숨기기", "Firebase 업데이트 실패: ${e.message}")
                                        }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("URL 숨기기", "쿼리 취소됨: ${error.message}")
                        }
                    })
            } catch (e: Exception) {
                Log.e("URL 숨기기 처리", e.toString())
            }
        }
    }

    /** showUrl - 숨겨진 URL 표시 */
    fun showUrl(url: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 로컬 DB 업데이트
                urlDao.updateUrlBackupHidden(url, false)

                // 2. Firebase 업데이트
                val userUrlRef = FirebaseDatabase.getInstance().reference
                    .child("User").child(userId).child("url")

                userUrlRef.orderByChild("url").equalTo(url)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (childSnapshot in snapshot.children) {
                                    childSnapshot.ref.child("hidden").setValue(false)
                                        .addOnSuccessListener {
                                            Log.d("URL 표시", "Firebase 업데이트 성공: $url")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("URL 표시", "Firebase 업데이트 실패: ${e.message}")
                                        }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("URL 표시", "쿼리 취소됨: ${error.message}")
                        }
                    })
            } catch (e: Exception) {
                Log.e("URL 표시 처리", e.toString())
            }
        }
    }

    /** getHiddenUrl - 숨겨진 URL 목록 조회 */
    fun getHiddenUrl(): LiveData<List<UrlBackupEntity>> = urlDao.getHiddenUrlBackups()

}
