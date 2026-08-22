package kr.baeksuk.urlbox.util.base

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.graphics.drawable.ColorDrawable
import android.text.format.Formatter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.DialogShareReceiveProgressBinding
import kr.baeksuk.urlBox.databinding.LayoutShareReceiveMinibarBinding
import kr.baeksuk.urlbox.model.Url
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

@SuppressLint("StaticFieldLeak")
object ShareReceiveMiniBarManager {

    private var initialized = false
    private var resumedActivityRef: WeakReference<Activity>? = null
    private val resumedActivity: Activity? get() = resumedActivityRef?.get()

    private var shareReceiveProgressDialog: AlertDialog? = null
    private var shareReceiveProgressBinding: DialogShareReceiveProgressBinding? = null
    private var shareReceiveMiniBarHost: ViewGroup? = null
    private var shareReceiveMiniBarView: View? = null
    private var shareReceiveMiniBarBinding: LayoutShareReceiveMinibarBinding? = null
    private val shareReceiveDotAnimators = mutableListOf<ObjectAnimator>()

    private var shareReceiveUrls: List<Url> = emptyList()
    private var shareReceiveProgressDone = 0
    private var shareReceiveProgressTotal = 0
    private var shareReceiveCurrentIndex = 0
    private var shareReceiveCurrentUrl: Url? = null
    private var shareReceiveHiddenByUser = false
    private var shareReceiveRunning = false
    private var suppressProgressDismissCallback = false

    fun init(application: Application) {
        if (initialized) return
        initialized = true

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityResumed(activity: Activity) {
                resumedActivityRef = WeakReference(activity)
                render()
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumedActivityRef?.get() === activity) {
                    dismissDialogsForActivityTransition()
                    resumedActivityRef = null
                }
            }

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                if (resumedActivityRef?.get() === activity) {
                    resumedActivityRef = null
                }
            }
        })
    }

    fun start(urls: List<Url>) {
        shareReceiveUrls = urls
        shareReceiveProgressDone = 0
        shareReceiveProgressTotal = urls.size
        shareReceiveCurrentIndex = 0
        shareReceiveCurrentUrl = urls.firstOrNull()
        shareReceiveHiddenByUser = false
        shareReceiveRunning = urls.isNotEmpty()
        render()
    }

    fun update(done: Int, total: Int, currentUrl: Url? = null, currentIndex: Int = 0) {
        shareReceiveProgressDone = done
        shareReceiveProgressTotal = total
        shareReceiveCurrentUrl = currentUrl
        shareReceiveCurrentIndex = currentIndex
        renderProgressContent()
        updateMiniBarContent(
            done,
            total,
            currentUrl,
            currentIndex,
            if (total <= 0) 0 else ((done * 100.0) / total).roundToInt()
        )
    }

    fun finish() {
        stopShareReceiveDotAnimation()
        dismissProgressDialog()
        dismissMiniBarDialog()
        clearState()
    }

    private fun clearState() {
        shareReceiveUrls = emptyList()
        shareReceiveProgressDone = 0
        shareReceiveProgressTotal = 0
        shareReceiveCurrentIndex = 0
        shareReceiveCurrentUrl = null
        shareReceiveHiddenByUser = false
        shareReceiveRunning = false
    }

    private fun dismissDialogsForActivityTransition() {
        stopShareReceiveDotAnimation()
        dismissProgressDialog()
        dismissMiniBarDialog()
    }

    private fun render() {
        if (!shareReceiveRunning || shareReceiveProgressTotal <= 0) {
            finish()
            return
        }
        val activity = resumedActivity ?: return

        if (shareReceiveHiddenByUser) {
            showShareReceiveMiniBar(activity)
        } else {
            showShareReceiveProgressDialog(activity)
        }
    }

    private fun showShareReceiveProgressDialog(activity: Activity) {
        dismissMiniBarDialog()
        dismissProgressDialog()

        val binding = DialogShareReceiveProgressBinding.inflate(LayoutInflater.from(activity))
        shareReceiveProgressBinding = binding
        renderShareReceiveThumbnails(activity, shareReceiveUrls)
        renderProgressContent()
        startShareReceiveDotAnimation()

        shareReceiveProgressDialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create().apply {
                setOnDismissListener {
                    if (suppressProgressDismissCallback) return@setOnDismissListener
                    shareReceiveProgressBinding = null
                    if (shareReceiveRunning && shareReceiveProgressDone < shareReceiveProgressTotal) {
                        shareReceiveHiddenByUser = true
                        render()
                    }
                }
                setCanceledOnTouchOutside(true)
                show()
                window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                window?.setGravity(Gravity.CENTER)
            }

        binding.btnShareProgressClose.setOnClickListener {
            shareReceiveHiddenByUser = true
            dismissProgressDialog()
            render()
        }
    }

    private fun renderProgressContent() {
        val binding = shareReceiveProgressBinding ?: return
        val activity = resumedActivity ?: return
        val percent = if (shareReceiveProgressTotal <= 0) 0 else ((shareReceiveProgressDone * 100.0) / shareReceiveProgressTotal).roundToInt()
        binding.pbShareProgress.max = 100
        binding.pbShareProgress.progress = percent
        binding.txShareDoneCount.text = "완료 ${shareReceiveProgressDone}장"
        binding.txShareTotalCount.text = "총 ${shareReceiveProgressTotal}장"
        binding.txShareItemStatus.text = if (shareReceiveProgressDone >= shareReceiveProgressTotal) "완료" else "불러오는 중..."
        binding.txShareItemName.text = shareReceiveCurrentUrl?.urlName?.takeIf { it.isNotBlank() } ?: "공유 링크"
        binding.txShareItemSize.text = resolveShareItemSize(activity, shareReceiveCurrentUrl)
        updateShareThumbnailHighlights(activity, shareReceiveCurrentIndex)
        loadProgressThumbnail(activity, shareReceiveCurrentUrl)
    }

    private fun loadProgressThumbnail(activity: Activity, url: Url?) {
        val binding = shareReceiveProgressBinding ?: return
        val imageSource = url?.imgUri?.takeIf { it.isNotBlank() }
        if (imageSource.isNullOrBlank()) {
            binding.ivShareItemThumbnail.setImageDrawable(null)
            return
        }
        Glide.with(activity)
            .load(imageSource)
            .centerCrop()
            .into(binding.ivShareItemThumbnail)
    }

    private fun renderShareReceiveThumbnails(activity: Activity, urls: List<Url>) {
        val binding = shareReceiveProgressBinding ?: return
        binding.llShareThumbnails.removeAllViews()
        urls.forEachIndexed { index, url ->
            val imageView = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(activity, 32), dp(activity, 32)).apply {
                    marginEnd = dp(activity, 6)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = activity.getDrawable(R.drawable.bg_share_thumbnail)
                clipToOutline = true
                alpha = if (index == shareReceiveCurrentIndex) 1f else 0.72f
            }
            binding.llShareThumbnails.addView(imageView)
            val imageSource = url.imgUri.takeIf { it.isNotBlank() }
            if (!imageSource.isNullOrBlank()) {
                Glide.with(activity)
                    .load(imageSource)
                    .centerCrop()
                    .into(imageView)
            }
        }
        updateShareThumbnailHighlights(activity, shareReceiveCurrentIndex)
    }

    private fun updateShareThumbnailHighlights(activity: Activity, activeIndex: Int) {
        val binding = shareReceiveProgressBinding ?: return
        val row = binding.llShareThumbnails
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            child.background = if (i == activeIndex) {
                activity.getDrawable(R.drawable.bg_share_thumbnail_selected)
            } else {
                activity.getDrawable(R.drawable.bg_share_thumbnail)
            }
            child.alpha = if (i == activeIndex) 1f else 0.72f
        }
    }

    private fun resolveShareItemSize(activity: Activity, url: Url?): String {
        val source = url?.imgUri?.takeIf { it.isNotBlank() } ?: return "썸네일 저장 중..."
        return try {
            val localFile = when {
                source.startsWith("content://") || source.startsWith("file://") -> null
                source.startsWith("http://") || source.startsWith("https://") -> null
                else -> java.io.File(source).takeIf { it.exists() && it.isFile }
            }
            if (localFile != null) Formatter.formatFileSize(activity, localFile.length()) else "썸네일 저장 중..."
        } catch (_: Exception) {
            "썸네일 저장 중..."
        }
    }

    private fun showShareReceiveMiniBar(activity: Activity) {
        dismissProgressDialog()
        dismissMiniBarDialog()

        val binding = LayoutShareReceiveMinibarBinding.inflate(LayoutInflater.from(activity))
        val host = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        shareReceiveMiniBarBinding = binding
        binding.clShareReceiveMinibar.scaleX = 0.92f
        binding.clShareReceiveMinibar.scaleY = 0.95f

        binding.root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> animateShareReceiveMiniBar(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateShareReceiveMiniBar(false)
            }
            false
        }
        binding.clShareReceiveMinibar.setOnClickListener {
            shareReceiveHiddenByUser = false
            render()
        }

        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            bottomMargin = resolveMiniBarBottomOffset(activity)
        }
        binding.root.alpha = 0f
        host.addView(binding.root, layoutParams)
        binding.root.animate().alpha(1f).setDuration(180L).start()
        shareReceiveMiniBarHost = host
        shareReceiveMiniBarView = binding.root

        updateMiniBarContent(
            shareReceiveProgressDone,
            shareReceiveProgressTotal,
            shareReceiveCurrentUrl,
            shareReceiveCurrentIndex,
            if (shareReceiveProgressTotal <= 0) 0 else ((shareReceiveProgressDone * 100.0) / shareReceiveProgressTotal).roundToInt()
        )
    }

    private fun animateShareReceiveMiniBar(pressed: Boolean) {
        val binding = shareReceiveMiniBarBinding ?: return
        val targetScale = if (pressed) 0.95f else 0.92f
        val targetYScale = if (pressed) 0.98f else 0.95f
        binding.clShareReceiveMinibar.animate()
            .scaleX(targetScale)
            .scaleY(targetYScale)
            .setDuration(if (pressed) 80L else 120L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun updateMiniBarContent(
        done: Int,
        total: Int,
        currentUrl: Url?,
        @Suppress("UNUSED_PARAMETER") currentIndex: Int,
        percent: Int
    ) {
        val binding = shareReceiveMiniBarBinding ?: return
        binding.txShareMinibarCount.text = "$done/$total"
        binding.pbShareMinibar.max = 100
        binding.pbShareMinibar.progress = percent
        binding.txShareMinibarTitle.text = "사진 저장 중..."
        binding.txShareMinibarSubtitle.text = currentUrl?.urlName?.takeIf { it.isNotBlank() } ?: "공유 링크"
        val imageSource = currentUrl?.imgUri?.takeIf { it.isNotBlank() }
        if (imageSource.isNullOrBlank()) {
            binding.ivShareMinibarThumbnail.setImageDrawable(null)
        } else {
            val activity = resumedActivity ?: return
            Glide.with(activity)
                .load(imageSource)
                .centerCrop()
                .into(binding.ivShareMinibarThumbnail)
        }
    }

    private fun startShareReceiveDotAnimation() {
        stopShareReceiveDotAnimation()
        val binding = shareReceiveProgressBinding ?: return
        val dots = listOf(
            binding.vShareLoadingDot1,
            binding.vShareLoadingDot2,
            binding.vShareLoadingDot3
        )
        dots.forEachIndexed { index, view ->
            val animator = ObjectAnimator.ofFloat(view, View.ALPHA, 0.35f, 1f, 0.35f).apply {
                duration = 900L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = index * 160L
            }
            shareReceiveDotAnimators.add(animator)
            animator.start()
        }
    }

    private fun stopShareReceiveDotAnimation() {
        shareReceiveDotAnimators.forEach { animator ->
            runCatching { animator.cancel() }
        }
        shareReceiveDotAnimators.clear()
    }

    private fun dismissProgressDialog() {
        stopShareReceiveDotAnimation()
        suppressProgressDismissCallback = true
        runCatching { shareReceiveProgressDialog?.dismiss() }
        suppressProgressDismissCallback = false
        shareReceiveProgressDialog = null
        shareReceiveProgressBinding = null
    }

    private fun dismissMiniBarDialog() {
        val miniBarView = shareReceiveMiniBarView
        val parent = miniBarView?.parent as? ViewGroup
        if (miniBarView != null && parent != null) {
            runCatching { parent.removeView(miniBarView) }
        }
        shareReceiveMiniBarView = null
        shareReceiveMiniBarHost = null
        shareReceiveMiniBarBinding = null
    }

    private fun resolveMiniBarBottomOffset(activity: Activity): Int {
        val baseMargin = activity.resources.getDimensionPixelSize(R.dimen.share_minibar_outer_margin_bottom)
        val bottomNav = activity.findViewById<View>(R.id.bottom_nav)
        val bottomNavHeight = when {
            bottomNav == null -> 0
            bottomNav.height > 0 -> bottomNav.height
            else -> activity.resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
        }
        val insetBottom = activity.window?.decorView?.rootWindowInsets?.systemWindowInsetBottom ?: 0
        val extraLift = if (bottomNav == null) {
            activity.resources.getDimensionPixelSize(R.dimen.share_minibar_extra_lift_without_bottom_nav)
        } else {
            activity.resources.getDimensionPixelSize(R.dimen.share_minibar_extra_lift_with_bottom_nav)
        }
        return baseMargin + bottomNavHeight + insetBottom + extraLift
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
