package org.fedorahosted.freeotp.messenger

import org.fedorahosted.freeotp.util.Settings
import java.lang.IllegalArgumentException
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

@Singleton
class MessengerFactory @Inject constructor(
    private val settings: Settings,
    private val executorService: ExecutorService,
) {

    @Throws(IllegalArgumentException::class)
    fun getMessenger(): Messenger {
        return when (settings.messenger) {
            MessengerType.TELEGRAM -> TelegramMessenger(settings, executorService)
            else -> throw IllegalArgumentException("No valid MessengerType is stored in Settings!")
        }
    }
}