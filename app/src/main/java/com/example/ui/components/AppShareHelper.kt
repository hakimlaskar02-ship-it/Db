package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

/**
 * Native Android sharing helper with automatic Clipboard fallback.
 *
 * Uses Android's native Share Sheet (WhatsApp, Telegram, SMS, etc.) and falls back to
 * copying the app link to the clipboard if no share target is available.
 */
object AppShareHelper {

    const val APP_SHARE_URL: String =
        "https://github.com/hakimlaskar02-ship-it/Db/releases"

    const val LINK_COPIED_TOAST: String =
        "App link copied! Paste it to share with friends."

    fun buildShareMessage(): String =
        "Check out DPBoss Smart Analytics! Use this link to open the app: $APP_SHARE_URL"

    fun shareApp(
        context: Context,
        onClipboardFallback: (String) -> Unit
    ) {
        val shareText = buildShareMessage()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DPBoss Smart Analytics")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share DPBoss Smart Analytics").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val canHandle = sendIntent.resolveActivity(context.packageManager) != null
        if (!canHandle) {
            copyAppLinkToClipboard(context, onClipboardFallback)
            return
        }
        runCatching {
            context.startActivity(chooser)
        }.onFailure {
            copyAppLinkToClipboard(context, onClipboardFallback)
        }
    }

    fun copyAppLinkToClipboard(
        context: Context,
        onCopied: (String) -> Unit
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("DPBoss Smart Analytics Link", APP_SHARE_URL)
        clipboard?.setPrimaryClip(clip)
        onCopied(LINK_COPIED_TOAST)
    }
}
