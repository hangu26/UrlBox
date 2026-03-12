package kr.baeksuk.urlbox.view.tutorial

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityTutorialBinding
import kr.baeksuk.urlbox.util.adapter.TutorialPagerAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.main.MainActivity

class TutorialActivity : BaseActivity() {

    private lateinit var tBinding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tBinding = DataBindingUtil.setContentView(this, R.layout.activity_tutorial)

        setTextSwipe()
        setupViewPager()
    }

    @SuppressLint("ClickableViewAccessibility", "CommitPrefEdits")
    private fun setupViewPager() {
        val bgViews = listOf(tBinding.bgPage1, tBinding.bgPage2, tBinding.bgPage3)

        tBinding.apply {
            lifecycleOwner = this@TutorialActivity
            activity = this@TutorialActivity

            viewPager.adapter = TutorialPagerAdapter(this@TutorialActivity)
            dotsIndicator.attachTo(viewPager)

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

                override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
                    super.onPageScrolled(position, offset, offsetPx)

                    if (position < bgViews.size - 1) {
                        bgViews[position].alpha = 1f - offset
                        bgViews[position + 1].alpha = offset
                    }
                }

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // 예: 마지막 페이지에서 버튼 텍스트 변경
                    if (position == bgViews.size - 1) {
                        txNext.text = "시작하기"
                    } else {
                        txNext.text = "다음"
                    }
                }
            })

            btnNextTutorial.setOnTouchListener { view, motionEvent ->
                setTouchAnimation(view, motionEvent)

                if (motionEvent.action == MotionEvent.ACTION_UP) {

                    val current = viewPager.currentItem
                    val last = viewPager.adapter?.itemCount?.minus(1) ?: 0

                    if (current < last) {
                        viewPager.setCurrentItem(current + 1, true)
                    } else {
                        val intent = Intent(this@TutorialActivity, MainActivity::class.java)
                        val prefs = getSharedPreferences("User", Context.MODE_PRIVATE)
                        prefs.edit().putInt("isClearIntent", 1).apply()
                        startActivityAnimation(intent, this@TutorialActivity)
                        finish()
                    }
                }

                true
            }
        }
    }

    /** 스와이프 텍스트 애니메이션 효과 **/
    private fun setTextSwipe() {

        val view = tBinding.txSwipeTutorial

        val moveRight = ObjectAnimator.ofFloat(view, "translationX", 0f, 55f)
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f)

        val moveBack = ObjectAnimator.ofFloat(view, "translationX", 55f, 0f)
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f)

        moveRight.duration = 900
        fadeIn.duration = 900
        moveBack.duration = 900
        fadeOut.duration = 900

        val set1 = AnimatorSet()
        set1.playTogether(moveRight, fadeIn)

        val set2 = AnimatorSet()
        set2.playTogether(moveBack, fadeOut)

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(set1, set2)
        animatorSet.startDelay = 500
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animatorSet.start()
            }
        })

        animatorSet.start()

    }

}