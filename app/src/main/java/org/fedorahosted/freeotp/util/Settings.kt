package org.fedorahosted.freeotp.util

import android.content.SharedPreferences
import org.fedorahosted.freeotp.messenger.MessengerType
import javax.inject.Inject
import javax.inject.Singleton

private const val DARK_MODE_KEY = "darkMode"
private const val COPY_TO_CLIPBOARD_KEY = "copyToClipboard"
private const val REQUIRE_AUTHENTICATION = "requireAuthentication"
private const val SEND_TO_MESSENGER = "sendToMessenger"
private const val MESSENGER = "messenger"
private const val MESSENGER_BOT_TOKEN = "messengerBotToken"
private const val MESSENGER_CHAT_ID = "messengerChatId"
private const val MESSENGER_DELETE_OTP_DELAY_MS = "messengerDeleteOtpDelay"

@Singleton
class Settings @Inject constructor(val sharedPreferences: SharedPreferences) {
    var darkMode: Boolean
        get() = sharedPreferences.getBoolean(DARK_MODE_KEY, false)
        set(value) = sharedPreferences.edit().putBoolean(DARK_MODE_KEY, value).apply()
    var copyToClipboard: Boolean
        get() = sharedPreferences.getBoolean(COPY_TO_CLIPBOARD_KEY, true)
        set(value) = sharedPreferences.edit().putBoolean(COPY_TO_CLIPBOARD_KEY, value).apply()
    var requireAuthentication: Boolean
        get() = sharedPreferences.getBoolean(REQUIRE_AUTHENTICATION, false)
        set(value) = sharedPreferences.edit().putBoolean(REQUIRE_AUTHENTICATION, value).apply()
    var sendToMessenger: Boolean
        get() = sharedPreferences.getBoolean(SEND_TO_MESSENGER, false)
        set(value) = sharedPreferences.edit().putBoolean(SEND_TO_MESSENGER, value).apply()
    var messenger: MessengerType?
        get() = sharedPreferences.getString(MESSENGER, null)?.let { MessengerType.valueOf(it) }
        set(value) = sharedPreferences.edit().putString(MESSENGER, value?.name).apply()
    var messengerBotToken: String
        get() = sharedPreferences.getString(MESSENGER_BOT_TOKEN, "")!!
        set(value) = sharedPreferences.edit().putString(MESSENGER_BOT_TOKEN, value).apply()
    var messengerChatId: String
        get() = sharedPreferences.getString(MESSENGER_CHAT_ID, "")!!
        set(value) = sharedPreferences.edit().putString(MESSENGER_CHAT_ID, value).apply()
    var messengerDeleteOtpDelayMs: Long
        get() = sharedPreferences.getLong(MESSENGER_DELETE_OTP_DELAY_MS, 10000)
        set(value) = sharedPreferences.edit().putLong(MESSENGER_DELETE_OTP_DELAY_MS, value).apply()

}