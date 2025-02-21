package kr.baeksuk.urlbox.view.addlink

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityAddLinkBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.AddLinkViewModel
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.koin.android.ext.android.inject


class AddLinkActivity : BaseActivity() {

    //노트북 작동 확인 커밋
    private lateinit var aBinding: ActivityAddLinkBinding
    private val aViewModel: AddLinkViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aBinding = DataBindingUtil.setContentView(this@AddLinkActivity, R.layout.activity_add_link)
        aBinding.apply {
            lifecycleOwner = this@AddLinkActivity
            viewmodel = aViewModel
            activity = this@AddLinkActivity
        }

        backPressedCallback.addCallbackActivity(this, MainActivity::class.java)
        observe()

    }

    private fun observe() = aViewModel.let { vm ->
        vm.btnCloseState.observe(this@AddLinkActivity) {
            if (it) {
                val intent = Intent(this@AddLinkActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@AddLinkActivity)
                finish()
            }
        }

        vm.urlInputDoneState.observe(this@AddLinkActivity) {
            if (it) {
                val intent = Intent(this@AddLinkActivity, CaptureActivity::class.java)
                intent.putExtra("url", aBinding.edtUrl.text.toString())

//                fetchMetadataFromUrl(aBinding.edtUrl.text.toString())

                startActivityAnimation(intent, this)
                finish()
            }
        }

    }

    /**
    fun fetchMetadataFromUrl(url: String) {
    // CoroutineScope로 백그라운드에서 실행
    CoroutineScope(Dispatchers.Main).launch {
    try {
    val result = withContext(Dispatchers.IO) {
    // Jsoup을 사용하여 HTML 문서를 가져옴
    val doc: Document = Jsoup.connect(url).get()

    // Open Graph 메타데이터 가져오기
    var title = doc.select("meta[property=og:title]").attr("content")
    var description = doc.select("meta[property=og:description]").attr("content")

    // 로그로 출력하여 메타데이터 확인
    Log.d("url 태그", "OG Title: $title, OG Description: $description")

    // 만약 Open Graph에서 타이틀과 설명을 못 찾았다면, 다른 태그를 시도
    if (title.isEmpty()) {
    title = doc.select("meta[name=twitter:title]").attr("content")
    Log.d("url 태그", "Twitter Title: $title")
    }
    if (description.isEmpty()) {
    description = doc.select("meta[name=twitter:description]").attr("content")
    Log.d("url 태그", "Twitter Description: $description")
    }

    // JSON-LD 데이터 가져오기
    if (title.isEmpty() || description.isEmpty()) {
    val jsonLdScript = doc.select("script[type=application/ld+json]")
    jsonLdScript.forEach {
    val jsonObject = JSONObject(it.data())
    // JSON-LD에서 title과 description을 추출
    val jsonTitle = jsonObject.optString("name", "")
    val jsonDescription = jsonObject.optString("description", "")
    Log.d("url 태그", "JSON-LD Title: $jsonTitle, JSON-LD Description: $jsonDescription")
    if (jsonTitle.isNotEmpty() && jsonDescription.isNotEmpty()) {
    return@withContext Pair(jsonTitle, jsonDescription)
    }
    }
    }

    // 추출된 title과 description이 있으면 반환
    if (title.isNotEmpty() && description.isNotEmpty()) {
    Log.d("url 태그", "Final Title: $title, Final Description: $description")
    return@withContext Pair(title, description)
    }

    null // 메타데이터를 찾을 수 없는 경우
    }

    // 결과 출력
    if (result != null) {
    Log.d("url 태그", "Fetched Metadata: $result")
    } else {
    Log.d("url 태그", "No metadata found")
    }
    } catch (e: Exception) {
    Log.e("url 태그", "Error occurred while fetching or parsing the metadata: ${e.message}")
    e.printStackTrace()
    }
    }
    }

     **/


}