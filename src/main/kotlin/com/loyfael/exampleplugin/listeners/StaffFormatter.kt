package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.universe.PlayerRef

val staffFormatter = object : PlayerChatEvent.Formatter {
    override fun format(playerRef: PlayerRef, message: String): Message {
        return Message.raw("[StaffChat] ${playerRef.username}: $message")
            .color("#ff3c00")
            .bold(true)
    }
}
