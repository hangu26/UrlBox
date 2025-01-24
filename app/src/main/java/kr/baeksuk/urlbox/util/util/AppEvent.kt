package kr.baeksuk.urlbox.util.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.LocalDate


fun MutableSharedFlow<Unit>.emit() = tryEmit(Unit)

object AppEvent {

    val onNavigation = MutableSharedFlow<LocalDate>(1, 0, BufferOverflow.DROP_OLDEST)

}