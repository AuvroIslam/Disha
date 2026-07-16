package com.example.gemmachat.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/** True when the app language is Bangla. Provided at the root from persisted app prefs. */
val LocalBangla = compositionLocalOf { false }

/** Pick the Bangla or English string for the current language. */
@Composable
@ReadOnlyComposable
fun tr(en: String, bn: String): String = if (LocalBangla.current) bn else en
