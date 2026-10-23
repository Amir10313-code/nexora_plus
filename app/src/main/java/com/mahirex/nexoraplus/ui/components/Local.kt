package com.mahirex.nexoraplus.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.mahirex.nexoraplus.core.locale.DigitStyle
import com.mahirex.nexoraplus.core.locale.ProfileFactory
import com.mahirex.nexoraplus.core.settings.AppSettings
import java.util.Locale

/** The calendar profile (language, region, calendars, week) every screen works with. */
val LocalProfile = staticCompositionLocalOf {
    ProfileFactory.build(AppSettings(), Locale.ENGLISH)
}

val LocalDigits = staticCompositionLocalOf { DigitStyle.LATIN }

/** A string resource with the digits of the current language applied. */
@Composable
fun loc(@StringRes id: Int, vararg args: Any): String {
    val raw = if (args.isEmpty()) stringResource(id) else stringResource(id, *args)
    return LocalDigits.current.localize(raw)
}

/** Localised number. */
@Composable
fun fmt(number: Int): String = LocalDigits.current.format(number)

@Composable
fun fmt(number: Long): String = LocalDigits.current.format(number)

@Composable
fun localizeDigits(text: String): String = LocalDigits.current.localize(text)
