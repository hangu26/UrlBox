package kr.baeksuk.urlbox.util.base

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<T : ViewDataBinding>(@LayoutRes val layoutRes: Int) : Fragment() {
    lateinit var binding: T

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = this@BaseFragment
        initView()
        super.onViewCreated(view, savedInstanceState)
    }

    abstract fun initView()

    fun startActivityAnimation(intent: Intent, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val options = ActivityOptions.makeCustomAnimation(
                context,
                androidx.appcompat.R.anim.abc_fade_in,
                androidx.appcompat.R.anim.abc_fade_out
            )
            context.startActivity(intent, options.toBundle())
            activity?.finish()
        } else {
            context.startActivity(intent)
            activity?.finish()
        }
    }

    fun setTouchAnimation(view: View, event: MotionEvent?) {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.97f).scaleY(0.97f).translationZ(5f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).translationZ(20f).setDuration(100).start()
                }
            }
        }
    }


    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(activity.window.decorView.applicationWindowToken, 0)

    }
}
