package kr.baeksuk.urlbox.util.base

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.viewmodel.share.ShareReceiverUiState
import kr.baeksuk.urlbox.viewmodel.share.ShareReceiverViewModel
import org.koin.android.ext.android.inject

class ShareReceiverActivity : AppCompatActivity() {

    private val viewModel: ShareReceiverViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeState()
        viewModel.handleSharedIntent(intent)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ShareReceiverUiState.Idle -> Unit
                        ShareReceiverUiState.Loading -> Log.d(TAG, "공유 인텐트 처리 중")
                        ShareReceiverUiState.Finished -> finish()
                        is ShareReceiverUiState.Error -> {
                            Log.e(TAG, state.message)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "공유 인텐트"
    }
}