package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.universe.PlayerRef

object StaffChatListener {
    private const val PREFIX = "!"

    /**
     * Permission node to manage via LuckPerms.
     *
     * Example:
     * - `/lp user <name> permission set loyfael.staffchat true`
     */
    private const val PERMISSION = "loyfael.staffchat"

    private val STAFF_FORMATTER = PlayerChatEvent.Formatter { playerRef: PlayerRef, message: String ->
        Message.raw("[Staff] ${playerRef.username}: $message")
            .color("#FFAA00")
            .bold(true)
    }

    fun onPlayerChat(event: PlayerChatEvent) {
        val rawMessage = event.readChatMessage() ?: return
        if (!rawMessage.startsWith(PREFIX)) return

        val stripped = rawMessage.removePrefix(PREFIX).trimStart()
        if (stripped.isBlank()) {
            // Prevent sending an empty staff message.
            event.trySetCancelled(true)
            event.trySendToSender(Message.raw("Usage: !<message>").color("#FF5555"))
            return
        }

        if (!event.senderHasPermission(PERMISSION)) {
            // If they don't have permission, keep normal behavior (let them use '!').
            return
        }

        // Staff-chat path: hide from global chat and send only to staff.
        event.trySetCancelled(false)
        event.trySetFormatter(STAFF_FORMATTER)
        event.trySetChatMessage(stripped)

        val recipients = event.readRecipients()
        if (recipients != null) {
            val staffRecipients = recipients.filter { ref -> ref.hasPermissionSafely(PERMISSION) }
            event.trySetRecipients(staffRecipients)
            return
        }

        // Fallback if recipients list is not exposed by this server build.
        // Cancel the normal chat event, and at least echo back to the sender.
        event.trySetCancelled(true)
        event.trySendToSender(
            Message.raw("[Staff] ${event.readSenderName() ?: "You"}: $stripped")
                .color("#FFAA00")
                .bold(true)
        )
    }
}

private fun PlayerChatEvent.readChatMessage(): String? {
    // Try common patterns without depending on the exact API.
    return tryInvoke<String>("getMessage")
        ?: tryGetProperty<String>("message")
        ?: tryGetProperty<String>("text")
}

private fun PlayerChatEvent.trySetChatMessage(message: String) {
    // Try common patterns without depending on the exact API.
    if (tryInvoke<Unit>("setMessage", message) != null) return
    trySetProperty("message", message)
    trySetProperty("text", message)
}

private fun PlayerChatEvent.readRecipients(): Collection<PlayerRef>? {
    @Suppress("UNCHECKED_CAST")
    return tryInvoke<Any>("getRecipients") as? Collection<PlayerRef>
        ?: tryGetProperty<Any>("recipients") as? Collection<PlayerRef>
}

private fun PlayerChatEvent.trySetRecipients(recipients: Collection<PlayerRef>) {
    if (tryInvoke<Unit>("setRecipients", recipients) != null) return
    trySetProperty("recipients", recipients)
}

private fun PlayerChatEvent.trySetFormatter(formatter: PlayerChatEvent.Formatter) {
    // Kotlin property is used in existing code, but use reflection here to be safe.
    if (tryInvoke<Unit>("setFormatter", formatter) != null) return
    trySetProperty("formatter", formatter)
}

private fun PlayerChatEvent.trySetCancelled(cancelled: Boolean) {
    if (tryInvoke<Unit>("setCancelled", cancelled) != null) return
    if (tryInvoke<Unit>("setIsCancelled", cancelled) != null) return
    trySetProperty("isCancelled", cancelled)
    trySetProperty("cancelled", cancelled)
}

private fun PlayerChatEvent.readSenderName(): String? {
    return tryInvoke<PlayerRef>("getSender")?.username
        ?: tryGetProperty<PlayerRef>("sender")?.username
        ?: tryInvoke<PlayerRef>("getPlayerRef")?.username
        ?: tryGetProperty<PlayerRef>("playerRef")?.username
}

private fun PlayerChatEvent.senderHasPermission(permission: String): Boolean {
    // Prefer PlayerRef.hasPermission if available.
    val sender = tryInvoke<PlayerRef>("getSender") ?: tryGetProperty("sender")
    if (sender != null && sender.hasPermissionSafely(permission)) return true

    // Some APIs expose a Player instance instead of / in addition to PlayerRef.
    val player = tryInvoke<Any>("getPlayer") ?: tryGetProperty<Any>("player")
    if (player != null && player.hasPermissionViaReflection(permission)) return true

    return false
}

private fun PlayerChatEvent.trySendToSender(message: Message) {
    val sender = tryInvoke<PlayerRef>("getSender") ?: tryGetProperty("sender")
    if (sender != null) {
        sender.sendMessage(message)
        return
    }

    val player = tryInvoke<Any>("getPlayer") ?: tryGetProperty<Any>("player")
    if (player != null) {
        // PlayerRef is the safe messaging path; fall back to reflection.
        player.tryInvoke<Unit>("sendMessage", message)
    }
}

private fun PlayerRef.hasPermissionSafely(permission: String): Boolean {
    return hasPermissionViaReflection(permission)
}

private fun Any.hasPermissionViaReflection(permission: String): Boolean {
    return tryInvoke<Boolean>("hasPermission", permission) == true
}

private inline fun <reified T> Any.tryInvoke(methodName: String, vararg args: Any?): T? {
    return runCatching {
        val argTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
        // First try exact match.
        val exact = javaClass.methods.firstOrNull { m ->
            m.name == methodName && m.parameterTypes.size == args.size
        }
        val method = exact ?: return@runCatching null
        @Suppress("UNCHECKED_CAST")
        method.invoke(this, *args) as? T
    }.getOrNull()
}

private inline fun <reified T> Any.tryGetProperty(propertyName: String): T? {
    return runCatching {
        val field = javaClass.fields.firstOrNull { it.name == propertyName }
            ?: javaClass.declaredFields.firstOrNull { it.name == propertyName }?.apply { isAccessible = true }
            ?: return@runCatching null
        @Suppress("UNCHECKED_CAST")
        field.get(this) as? T
    }.getOrNull()
}

private fun Any.trySetProperty(propertyName: String, value: Any?) {
    runCatching {
        val field = javaClass.fields.firstOrNull { it.name == propertyName }
            ?: javaClass.declaredFields.firstOrNull { it.name == propertyName }?.apply { isAccessible = true }
            ?: return@runCatching
        field.set(this, value)
    }
}
