package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.universe.PlayerRef
import net.luckperms.api.LuckPermsProvider
import java.util.UUID

object StaffChatListener {
    private const val PREFIX = "!"

    /**
     * Permission node to manage via LuckPerms.
     *
     * Example:
     * - `/lp user <name> permission set loyfael.staffchat true`
     */
    private const val PERMISSION = "loyfael.staffchat"

    private val staffFormatter = object : PlayerChatEvent.Formatter {
        override fun format(playerRef: PlayerRef, message: String): Message {
            return Message.raw("[Staff] ${playerRef.username}: $message")
                .color("#FFAA00")
                .bold(true)
        }
    }

    fun onPlayerChat(event: PlayerChatEvent) {
        val rawMessage = readChatMessage(event)
        if (rawMessage == null) return
        if (!rawMessage.startsWith(PREFIX)) return

        val stripped = rawMessage.removePrefix(PREFIX).trimStart()
        if (stripped.isBlank()) {
            // Prevent sending an empty staff message.
            setCancelled(event, true)
            sendToSender(event, Message.raw("Usage: !<message>").color("#FF5555"))
            return
        }

        if (!senderHasPermission(event, PERMISSION)) {
            // If they don't have permission, keep normal behavior (let them use '!').
            return
        }

        // Staff-chat path: hide from global chat and send only to staff.
        setCancelled(event, false)
        setFormatter(event, staffFormatter)
        setChatMessage(event, stripped)

        val recipients = readRecipients(event)
        if (recipients != null) {
            val staffRecipients = recipients.filter { ref -> hasPermissionForRef(ref, PERMISSION) }
            setRecipients(event, staffRecipients)
            return
        }

        // Fallback if recipients list is not exposed by this server build.
        // Cancel the normal chat event, and at least echo back to the sender.
        setCancelled(event, true)
        val senderName = readSenderName(event) ?: "You"
        sendToSender(
            event,
            Message.raw("[Staff] $senderName: $stripped")
                .color("#FFAA00")
                .bold(true)
        )
    }

    private fun readChatMessage(event: PlayerChatEvent): String? {
        // Try common patterns without depending on the exact API.
        val fromGetter = tryInvoke(event, "getMessage") as? String
        if (fromGetter != null) return fromGetter

        val fromMessageField = tryGetField(event, "message") as? String
        if (fromMessageField != null) return fromMessageField

        return tryGetField(event, "text") as? String
    }

    private fun setChatMessage(event: PlayerChatEvent, message: String) {
        // Try common patterns without depending on the exact API.
        if (tryInvoke(event, "setMessage", message) != null) return
        trySetField(event, "message", message)
        trySetField(event, "text", message)
    }

    private fun readRecipients(event: PlayerChatEvent): Collection<PlayerRef>? {
        @Suppress("UNCHECKED_CAST")
        val fromGetter = tryInvoke(event, "getRecipients") as? Collection<PlayerRef>
        if (fromGetter != null) return fromGetter

        @Suppress("UNCHECKED_CAST")
        return tryGetField(event, "recipients") as? Collection<PlayerRef>
    }

    private fun setRecipients(event: PlayerChatEvent, recipients: Collection<PlayerRef>) {
        if (tryInvoke(event, "setRecipients", recipients) != null) return
        trySetField(event, "recipients", recipients)
    }

    private fun setFormatter(event: PlayerChatEvent, formatter: PlayerChatEvent.Formatter) {
        // Kotlin property is used in existing code, but use reflection here to be safe.
        if (tryInvoke(event, "setFormatter", formatter) != null) return
        trySetField(event, "formatter", formatter)
    }

    private fun setCancelled(event: PlayerChatEvent, cancelled: Boolean) {
        if (tryInvoke(event, "setCancelled", cancelled) != null) return
        if (tryInvoke(event, "setIsCancelled", cancelled) != null) return
        trySetField(event, "isCancelled", cancelled)
        trySetField(event, "cancelled", cancelled)
    }

    private fun readSenderName(event: PlayerChatEvent): String? {
        val sender = getSenderRef(event)
        if (sender != null) return sender.username

        val playerRef = tryInvoke(event, "getPlayerRef") as? PlayerRef
        if (playerRef != null) return playerRef.username

        return (tryGetField(event, "playerRef") as? PlayerRef)?.username
    }

    private fun senderHasPermission(event: PlayerChatEvent, permission: String): Boolean {
        // Prefer LuckPerms if it's installed and we can resolve a UUID.
        val senderRef = getSenderRef(event)

        var senderUuid: UUID? = null
        if (senderRef != null) {
            senderUuid = readUuid(senderRef)
        }
        if (senderUuid == null) {
            val playerObject = tryInvoke(event, "getPlayer") ?: tryGetField(event, "player")
            if (playerObject != null) {
                senderUuid = readUuid(playerObject)
            }
        }

        if (senderUuid != null) {
            val lpResult = hasPermissionViaLuckPerms(senderUuid!!, permission)
            if (lpResult != null) return lpResult
        }

        // Prefer PlayerRef.hasPermission if available.
        val sender = getSenderRef(event)
        if (sender != null && hasPermissionFallback(sender, permission)) return true

        // Some APIs expose a Player instance instead of / in addition to PlayerRef.
        val player = tryInvoke(event, "getPlayer") ?: tryGetField(event, "player")
        if (player != null && hasPermissionFallback(player, permission)) return true

        return false
    }

    private fun sendToSender(event: PlayerChatEvent, message: Message) {
        val sender = getSenderRef(event)
        if (sender != null) {
            sender.sendMessage(message)
            return
        }

        val player = tryInvoke(event, "getPlayer") ?: tryGetField(event, "player")
        if (player != null) {
            // PlayerRef is the safe messaging path; fall back to reflection.
            tryInvoke(player, "sendMessage", message)
        }
    }

    private fun getSenderRef(event: PlayerChatEvent): PlayerRef? {
        val fromGetter = tryInvoke(event, "getSender") as? PlayerRef
        if (fromGetter != null) return fromGetter
        return tryGetField(event, "sender") as? PlayerRef
    }

    private fun hasPermissionForRef(ref: PlayerRef, permission: String): Boolean {
        val uuid = readUuid(ref)
        if (uuid != null) {
            val lpResult = hasPermissionViaLuckPerms(uuid, permission)
            if (lpResult != null) return lpResult
        }
        return hasPermissionFallback(ref, permission)
    }

    private fun hasPermissionFallback(target: Any, permission: String): Boolean {
        return (tryInvoke(target, "hasPermission", permission) as? Boolean) == true
    }

    private fun hasPermissionViaLuckPerms(uuid: UUID, permission: String): Boolean? {
        // Returns:
        // - true/false: LuckPerms present + user cached + definitive answer
        // - null: LuckPerms not present / not loaded / no cached user -> caller should fall back
        val api = try {
            LuckPermsProvider.get()
        } catch (_: Throwable) {
            return null
        }

        val user = api.userManager.getUser(uuid)
        if (user == null) return null

        val queryOptions = try {
            api.contextManager.getQueryOptions(user)
                .orElseGet { api.contextManager.staticQueryOptions }
        } catch (_: Throwable) {
            // Very defensive: if the API shape differs, fall back.
            return null
        }

        return try {
            user.cachedData
                .getPermissionData(queryOptions)
                .checkPermission(permission)
                .asBoolean()
        } catch (_: Throwable) {
            null
        }
    }

    private fun readUuid(target: Any): UUID? {
        val candidates = arrayOf(
            tryInvoke(target, "getUniqueId"),
            tryInvoke(target, "getUuid"),
            tryGetField(target, "uniqueId"),
            tryGetField(target, "uuid"),
        )

        for (value in candidates) {
            when (value) {
                is UUID -> return value
                is String -> {
                    try {
                        return UUID.fromString(value)
                    } catch (_: IllegalArgumentException) {
                        // Ignore and keep trying
                    }
                }
            }
        }
        return null
    }

    private fun tryInvoke(target: Any, methodName: String, vararg args: Any?): Any? {
        return try {
            val method = target.javaClass.methods.firstOrNull { m ->
                m.name == methodName && m.parameterTypes.size == args.size
            } ?: return null
            method.invoke(target, *args)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Gets a field value via reflection, ignoring failures.
     * Used for compatibility across different server versions.
     * 
     */
    private fun tryGetField(target: Any, fieldName: String): Any? {
        return try {
            val publicField = target.javaClass.fields.firstOrNull { it.name == fieldName }
            if (publicField != null) return publicField.get(target)

            val declaredField = target.javaClass.declaredFields.firstOrNull { it.name == fieldName }
                ?: return null
            declaredField.isAccessible = true
            declaredField.get(target)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Sets a field value via reflection, ignoring failures.
     * Used for compatibility across different server versions.
     * 
     * For beginner: don't use this unless necessary!
     */
    private fun trySetField(target: Any, fieldName: String, value: Any?) {
        try {
            val publicField = target.javaClass.fields.firstOrNull { it.name == fieldName }
            if (publicField != null) {
                publicField.set(target, value)
                return
            }

            val declaredField = target.javaClass.declaredFields.firstOrNull { it.name == fieldName }
                ?: return
            declaredField.isAccessible = true
            declaredField.set(target, value)
        } catch (_: Throwable) {
            // Ignore: compatibility helper
        }
    }
}
