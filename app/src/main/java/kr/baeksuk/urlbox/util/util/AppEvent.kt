package kr.baeksuk.urlbox.util.util

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate


fun MutableSharedFlow<Unit>.emit() = tryEmit(Unit)

object AppEvent {

    val onNavigation = MutableSharedFlow<LocalDate>(1, 0, BufferOverflow.DROP_OLDEST)
    val onHiddenToggle = MutableSharedFlow<Boolean>(1, 0, BufferOverflow.DROP_OLDEST)
    val showHiddenState = MutableStateFlow(false)

}

// Unified secret logger for developer debugging
fun secretLog(message: String) {
    Log.e("SecretLog", message)
}