package org.fedorahosted.freeotp.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.fedorahosted.freeotp.R
import org.fedorahosted.freeotp.databinding.ConfigMessengerBinding
import org.fedorahosted.freeotp.databinding.EditBinding
import org.fedorahosted.freeotp.messenger.MessengerType
import org.fedorahosted.freeotp.util.Settings
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ConfigureMessengerActivity : AppCompatActivity(), TextWatcher, View.OnClickListener {

    @Inject
    lateinit var settings: Settings

    private lateinit var binding: ConfigMessengerBinding
    private lateinit var mMessengerType: Spinner
    private lateinit var mBotToken: EditText
    private lateinit var mChatId: EditText
    private lateinit var mOtpDeleteDelay: EditText
    private lateinit var mSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ConfigMessengerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {

            // Get references to widgets.
            mMessengerType = findViewById<View>(R.id.messenger_type_select) as Spinner
            val messengerTypes = MessengerType.values()
            mMessengerType.setAdapter(
                ArrayAdapter(
                    baseContext,
                    android.R.layout.simple_list_item_1,
                    messengerTypes
                )
            )
            mBotToken = findViewById<View>(R.id.bot_token_text) as EditText
            mChatId = findViewById<View>(R.id.chat_id_text) as EditText
            mOtpDeleteDelay = findViewById<View>(R.id.otp_delete_delay_text) as EditText
            mSave = findViewById<View>(R.id.save) as Button
            setSupportActionBar(findViewById(R.id.toolbar))

            // Select MessengerType from settings or first as default
            mMessengerType.setSelection(settings.messenger?.let { messengerTypes.indexOf(it) } ?: 0)
            mBotToken.setText(settings.messengerBotToken)
            mChatId.setText(settings.messengerChatId)
            mOtpDeleteDelay.setText(settings.messengerDeleteOtpDelayMs.toString())

            // Setup text changed listeners.
            mBotToken.addTextChangedListener(this@ConfigureMessengerActivity)
            mChatId.addTextChangedListener(this@ConfigureMessengerActivity)
            mOtpDeleteDelay.addTextChangedListener(this@ConfigureMessengerActivity)

            // Setup click callbacks.
            findViewById<View>(R.id.cancel).setOnClickListener(this@ConfigureMessengerActivity)
            findViewById<View>(R.id.save).setOnClickListener(this@ConfigureMessengerActivity)

        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val botToken = mBotToken.text.toString()
        val chatId = mChatId.text.toString()
        val otpDeleteDelay = mOtpDeleteDelay.text.toString()
        mSave.isEnabled =
            botToken.isNotEmpty() && chatId.isNotEmpty() && otpDeleteDelay.isNotEmpty()
    }

    override fun afterTextChanged(s: Editable) {

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.save -> {
                lifecycleScope.launch {
                    settings.messenger = mMessengerType.selectedItem as MessengerType
                    settings.messengerBotToken = mBotToken.text.toString()
                    settings.messengerChatId = mChatId.text.toString()
                    settings.messengerDeleteOtpDelayMs = mOtpDeleteDelay.text.toString().toLong()
                    setResult(RESULT_OK)
                    finish()
                }
            }

            R.id.cancel -> finish()
        }
    }

}
