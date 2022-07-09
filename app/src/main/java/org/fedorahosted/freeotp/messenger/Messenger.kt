package org.fedorahosted.freeotp.messenger

import android.util.Log
import org.fedorahosted.freeotp.util.Settings
import java.util.concurrent.ExecutorService

abstract class Messenger(
    val settings: Settings,
    val executorService: ExecutorService
) {
    private val tag = Messenger::class.java.simpleName

    /**
     * Try to send given `otp` to
     * @param onSuccess is called if sending to Messenger succeeded
     * @param onError is called with the error message if sending to Messenger failed
     */
    @Throws(RuntimeException::class)
    fun sendOTP(
        otp: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((msg: String) -> Unit)? = null,
    ) {
        Log.i(tag, "Send OTP $otp to Messenger")
        executorService.execute {
            executeSendOtp(otp, onSuccess, onError)
        }
    }

    abstract fun executeSendOtp(
        otp: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((msg: String) -> Unit)? = null
    )
}