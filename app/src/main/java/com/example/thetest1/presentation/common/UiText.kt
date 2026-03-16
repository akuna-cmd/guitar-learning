package com.example.thetest1.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Res(val resId: Int) : UiText
}

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.Plain -> value
        is UiText.Res -> stringResource(id = resId)
    }
}
