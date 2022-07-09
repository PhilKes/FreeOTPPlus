package org.fedorahosted.freeotp.messenger

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.response.SendResponse
import org.fedorahosted.freeotp.messenger.Messenger
import org.fedorahosted.freeotp.util.Settings
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

class TelegramMessenger(settings: Settings, executorService: ExecutorService) :
    Messenger(settings, executorService) {

    private fun getTelegramBot(): TelegramBot = TelegramBot(settings.messengerBotToken)

    @Throws(java.lang.RuntimeException::class)
    override fun executeSendOtp(
        otp: String,
        onSuccess: (() -> Unit)?,
        onError: ((msg: String) -> Unit)?
    ) {
        val telegramBot = getTelegramBot()
        val sendResponse: SendResponse =
            telegramBot.execute(
                SendMessage(settings.messengerChatId, "`$otp`").parseMode(ParseMode.MarkdownV2)
            )
        if (!sendResponse.isOk) {
            onError?.invoke("Send OTP to Telegram failed: Error ${sendResponse.errorCode()}: ${sendResponse.description()}")
            return
        }
        // Delete OTP message after 10 Seconds
        executorService.execute {
            Thread.sleep(settings.messengerDeleteOtpDelayMs)
            val deleteResponse: BaseResponse =
                telegramBot.execute(
                    DeleteMessage(
                        settings.messengerChatId,
                        sendResponse.message().messageId()
                    )
                )
            if (!deleteResponse.isOk) {
                onError?.invoke("Could not delete OTP message from Telegram Chat: Error ${deleteResponse.errorCode()}: ${deleteResponse.description()}")
            }
        }
        onSuccess?.invoke()
    }
}