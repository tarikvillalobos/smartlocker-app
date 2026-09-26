package app.smartlocker.design

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** Repositions the whole field when the keyboard or window changes around focus. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepAboveKeyboard(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val keyboardBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val windowSize = LocalWindowInfo.current.containerSize
    LaunchedEffect(focused, keyboardBottom, windowSize, fieldSize) {
        if (focused && keyboardBottom > 0 && fieldSize != IntSize.Zero) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    return bringIntoViewRequester(requester)
        .onSizeChanged { fieldSize = it }
        .onFocusChanged { focused = it.hasFocus }
}
