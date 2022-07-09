package org.fedorahosted.freeotp.messenger

enum class MessengerType(val id: Int) {
    TELEGRAM(1);

    override fun toString() = name.lowercase().replaceFirstChar { it.titlecase() }

}