package kr.baeksuk.urlbox.view.tutorial.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentFirstTutoBinding
import kr.baeksuk.urlbox.util.base.BaseFragment

class FirstTutoFragment : BaseFragment<FragmentFirstTutoBinding>(R.layout.fragment_first_tuto) {

    override fun initView() {
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            fragment = this@FirstTutoFragment
        }
    }


}