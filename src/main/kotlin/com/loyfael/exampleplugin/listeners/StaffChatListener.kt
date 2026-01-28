package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent

object StaffChatListener {
    private const val PREFIX = "!"


    /**
     * Permission node (loyfael.staffchat) managed via LuckPerms.
     *
     * Example:
     * - `/lp user <name> permission set loyfael.staffchat true`
     */


    fun onPlayerChat(event: PlayerChatEvent) {
        Log.dbg("onPlayerChat invoked: event=${event.javaClass.name}")
        val rawMessage = readChatMessage(event)
        Log.dbg("readChatMessage -> '${rawMessage ?: "<null>"}'")
        if (rawMessage == null) return
        if (!rawMessage.startsWith(PREFIX)) return

        val stripped = rawMessage.removePrefix(PREFIX).trimStart()
        if (stripped.isBlank()) {
            // Prevent sending an empty staff message.
            Log.dbg("empty staff message -> cancel and notify sender")
            setCancelled(event, true)
            sendToSender(event, Message.raw("Usage: !<message>").color("#FF5555"))
            return
        }

        if (!senderHasPermission(event)) {
            Log.dbg("senderHasPermission -> false, aborting staff chat path")
            // If they don't have permission, keep normal behavior (let them use '!').
            return
        }
        Log.dbg("senderHasPermission -> true, proceeding with staff chat path")

        // Staff-chat path: cancel original broadcast and send only to staff ourselves.
        setCancelled(event, true)

        val recipients = readRecipients(event)
        Log.dbg("readRecipients -> ${if (recipients == null) "<null>" else "${recipients.size} recipients"}")
        if (recipients != null) {
            val staffRecipients = recipients.filter { ref -> hasPermissionForRef(ref) }
            Log.dbg("filtered staffRecipients -> ${staffRecipients.size}")

            // après avoir calculé staffRecipients
            if (staffRecipients.isNotEmpty()) {
                // Cancel original event so the normal broadcast doesn't occur
                setCancelled(event, true)

                val senderName = readSenderName(event) ?: "You"
                val msg = Message.raw("[StaffChat] $senderName: $stripped")
                    .color("#ff3c00")
                    .bold(true)

                // send to each staff member directly
                for (ref in staffRecipients) {
                    try {
                        ref.sendMessage(msg)
                    } catch (_: Throwable) {
                        // fallback: use reflection if PlayerRef api differs
                        tryInvoke(ref, "sendMessage", msg)
                    }
                }

                // avoid duplicating message to sender if they're already in recipients
                val senderRef = getSenderRef(event)
                if (senderRef == null || staffRecipients.none { r -> readUuid(r) == readUuid(senderRef) }) {
                    sendToSender(event, msg)
                }

                return
            }
        }

        // Fallback if recipients list is not exposed by this server build.
        // Cancel the normal chat event, and at least echo back to the sender.
        setCancelled(event, true)
        val senderName = readSenderName(event) ?: "You"
        Log.dbg("fallback: echoing back to sender '$senderName'")
        sendToSender(
            event,
            Message.raw("[Staff] $senderName: $stripped")
                .color("#FFAA00")
                .bold(true)
        )
    }

}