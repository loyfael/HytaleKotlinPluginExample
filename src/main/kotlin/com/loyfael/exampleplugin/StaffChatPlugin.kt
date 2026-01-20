package com.loyfael.exampleplugin

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.loyfael.exampleplugin.listeners.StaffChatListener

class StaffChatPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    override fun setup() {
        eventRegistry.registerGlobal(PlayerChatEvent::class.java) { event ->
            StaffChatListener.onPlayerChat(event)
        }
    }
}
