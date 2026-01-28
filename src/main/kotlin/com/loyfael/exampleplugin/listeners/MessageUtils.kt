package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.universe.PlayerRef

fun readChatMessage(event: PlayerChatEvent): String? {
    // Try common patterns without depending on the exact API.
    val fromGetterAny = tryInvoke(event, "getMessage")
    if (fromGetterAny != null) {
        when (fromGetterAny) {
            is String -> {
                Log.dbg("readChatMessage: getMessage() -> '$fromGetterAny'")
                return fromGetterAny
            }
            is Message -> {
                val s = tryInvoke(fromGetterAny, "getText") as? String
                    ?: tryInvoke(fromGetterAny, "toString") as? String
                    ?: fromGetterAny.toString()
                Log.dbg("readChatMessage: getMessage() -> Message -> '$s'")
                return s
            }
            else -> {
                val s = fromGetterAny.toString()
                if (s.isNotBlank()) {
                    Log.dbg("readChatMessage: getMessage() -> (toString) '$s'")
                    return s
                }
            }
        }
    }

    val fromMessageFieldAny = tryGetField(event, "message")
    if (fromMessageFieldAny != null) {
        when (fromMessageFieldAny) {
            is String -> {
                Log.dbg("readChatMessage: field 'message' -> '$fromMessageFieldAny'")
                return fromMessageFieldAny
            }
            is Message -> {
                val s = tryInvoke(fromMessageFieldAny, "getText") as? String
                    ?: tryInvoke(fromMessageFieldAny, "toString") as? String
                    ?: fromMessageFieldAny.toString()
                Log.dbg("readChatMessage: field 'message' -> Message -> '$s'")
                return s
            }
            else -> {
                val s = fromMessageFieldAny.toString()
                if (s.isNotBlank()) {
                    Log.dbg("readChatMessage: field 'message' -> (toString) '$s'")
                    return s
                }
            }
        }
    }

    val contentGetter = tryInvoke(event, "getContent") as? String
    if (contentGetter != null) {
        Log.dbg("readChatMessage: getContent() -> '$contentGetter'")
        return contentGetter
    }

    val contentField = tryGetField(event, "content") as? String
    if (contentField != null) {
        Log.dbg("readChatMessage: field 'content' -> '$contentField'")
        return contentField
    }

    val fromGetText = tryInvoke(event, "getText") as? String
    if (fromGetText != null) {
        Log.dbg("readChatMessage: getText() -> '$fromGetText'")
        return fromGetText
    }

    val textFieldAny = tryGetField(event, "text")
    if (textFieldAny != null) {
        when (textFieldAny) {
            is String -> {
                Log.dbg("readChatMessage: field 'text' -> '$textFieldAny'")
                return textFieldAny
            }
            is Message -> {
                val s = tryInvoke(textFieldAny, "getText") as? String
                    ?: tryInvoke(textFieldAny, "toString") as? String
                    ?: textFieldAny.toString()
                Log.dbg("readChatMessage: field 'text' -> Message -> '$s'")
                return s
            }
            else -> {
                val s = textFieldAny.toString()
                if (s.isNotBlank()) {
                    Log.dbg("readChatMessage: field 'text' -> (toString) '$s'")
                    return s
                }
            }
        }
    }

    // try a few extra candidate getters
    for (name in listOf("getChatMessage","getRawMessage","getMessageText","getMessageString")) {
        val res = tryInvoke(event, name)
        if (res is String) {
            Log.dbg("readChatMessage: $name -> '$res'")
            return res
        }
        if (res is Message) {
            val s = tryInvoke(res, "getText") as? String ?: res.toString()
            Log.dbg("readChatMessage: $name -> Message -> '$s'")
            return s
        }
        if (res != null) {
            val s = res.toString()
            if (s.isNotBlank()) {
                Log.dbg("readChatMessage: $name -> (toString) '$s'")
                return s
            }
        }
    }

    Log.dbg("readChatMessage: couldn't find message, dumping event info")
    dumpEventInfo(event)
    return null
}

fun setChatMessage(event: PlayerChatEvent, message: String) {
    Log.dbg("setChatMessage: attempting to set message='$message'")
    if (tryInvoke(event, "setMessage", message) != null) return
    if (tryInvoke(event, "setContent", message) != null) return
    // Some APIs expect a Message object
    val msgObj = Message.raw(message)
    if (tryInvoke(event, "setMessage", msgObj) != null) return
    if (tryInvoke(event, "setContent", msgObj) != null) return
    if (tryInvoke(event, "setText", message) != null) return
    trySetField(event, "message", message)
    trySetField(event, "content", message)
    trySetField(event, "message", msgObj)
    trySetField(event, "content", msgObj)
    trySetField(event, "text", message)
    trySetField(event, "text", msgObj)
}

fun readRecipients(event: PlayerChatEvent): Collection<PlayerRef>? {
    @Suppress("UNCHECKED_CAST")
    val fromGetter = tryInvoke(event, "getRecipients") as? Collection<PlayerRef>
    if (fromGetter != null) {
        Log.dbg("readRecipients: getRecipients() -> ${'$'}{fromGetter.size}")
        return fromGetter
    }

    @Suppress("UNCHECKED_CAST")
    val fromField = tryGetField(event, "recipients") as? Collection<PlayerRef>
    if (fromField != null) {
        Log.dbg("readRecipients: field 'recipients' -> ${'$'}{fromField.size}")
        return fromField
    }

    // New: support getTargets/targets
    @Suppress("UNCHECKED_CAST")
    val fromTargets = tryInvoke(event, "getTargets") as? Collection<PlayerRef>
    if (fromTargets != null) {
        Log.dbg("readRecipients: getTargets() -> ${'$'}{fromTargets.size}")
        return fromTargets
    }
    @Suppress("UNCHECKED_CAST")
    val targetsField = tryGetField(event, "targets") as? Collection<PlayerRef>
    if (targetsField != null) {
        Log.dbg("readRecipients: field 'targets' -> ${'$'}{targetsField.size}")
        return targetsField
    }

    return null
}

fun setRecipients(event: PlayerChatEvent, recipients: Collection<PlayerRef>) {
    Log.dbg("setRecipients: ${'$'}{recipients.size} recipients")
    if (tryInvoke(event, "setRecipients", recipients) != null) return
    if (tryInvoke(event, "setTargets", recipients) != null) return
    trySetField(event, "recipients", recipients)
    trySetField(event, "targets", recipients)
}

fun setFormatter(event: PlayerChatEvent) {
    Log.dbg("setFormatter: applying staff formatter")
    if (tryInvoke(event, "setFormatter", staffFormatter) != null) return
    trySetField(event, "formatter", staffFormatter)
}

fun dumpEventInfo(event: Any) {
    try {
        Log.dbg("dumpEventInfo: class=${'$'}{event.javaClass.name}")
        val ts = try { event.toString() } catch (t: Throwable) { "<toString threw ${'$'}{t::class.java.name}>" }
        Log.dbg("dumpEventInfo: toString() -> $ts")

        val methods = event.javaClass.methods
            .filter { m -> listOf("get", "is", "read", "set").any { m.name.startsWith(it, ignoreCase = true) } }
            .sortedBy { it.name }
            .map { m -> "${'$'}{m.name}(${'$'}{m.parameterTypes.size})" }
            .take(80)
        val methodsSummary = methods.joinToString(", ")
        Log.dbg("dumpEventInfo: methods (${ '$' }{methods.size}) -> $methodsSummary")

        val fields = event.javaClass.declaredFields
            .map { f -> "${ '$' }{f.name}:${ '$' }{f.type.simpleName}" }
            .take(80)
        val fieldsSummary = fields.joinToString(", ")
        Log.dbg("dumpEventInfo: declaredFields (${ '$' }{fields.size}) -> $fieldsSummary")

        // quick presence checks
        val hasRead = event.javaClass.methods.any { it.name == "readChatMessage" || it.name == "getMessage" || it.name == "getText" }
        Log.dbg("dumpEventInfo: hasRead/getMessage/getText -> $hasRead")

        // try to invoke candidate readers safely
        val candidates = listOf("readChatMessage", "getMessage", "getText")
        for (name in candidates) {
            val m = event.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
            if (m != null) {
                val res = try {
                    m.invoke(event)
                } catch (t: Throwable) {
                    "threw ${'$'}{t::class.java.simpleName}"
                }
                Log.dbg("dumpEventInfo: invoke $name -> $res")
            }
        }
    } catch (t: Throwable) {
        Log.dbg("dumpEventInfo: failed: ${'$'}{t::class.java.name}: ${'$'}{t.message}")
    }
}

fun setCancelled(event: PlayerChatEvent, cancelled: Boolean) {
    Log.dbg("setCancelled: $cancelled")
    if (tryInvoke(event, "setCancelled", cancelled) != null) return
    if (tryInvoke(event, "setIsCancelled", cancelled) != null) return
    trySetField(event, "isCancelled", cancelled)
    trySetField(event, "cancelled", cancelled)
}

fun readSenderName(event: PlayerChatEvent): String? {
    val sender = getSenderRef(event)
    if (sender != null) {
        Log.dbg("readSenderName: senderRef -> ${'$'}{sender.username}")
        return sender.username
    }

    val playerRef = tryInvoke(event, "getPlayerRef") as? PlayerRef
    if (playerRef != null) {
        Log.dbg("readSenderName: getPlayerRef() -> ${'$'}{playerRef.username}")
        return playerRef.username
    }

    val pf = tryGetField(event, "playerRef") as? PlayerRef
    if (pf != null) Log.dbg("readSenderName: field playerRef -> ${'$'}{pf.username}")
    return pf?.username
}

fun sendToSender(event: PlayerChatEvent, message: Message) {
    val sender = getSenderRef(event)
    if (sender != null) {
        Log.dbg("sendToSender: using PlayerRef -> ${'$'}{sender.username}")
        sender.sendMessage(message)
        return
    }

    val player = tryInvoke(event, "getPlayer") ?: tryGetField(event, "player")
    if (player != null) {
        Log.dbg("sendToSender: invoking player.sendMessage via reflection")
        tryInvoke(player, "sendMessage", message)
    } else {
        Log.dbg("sendToSender: no sender/player available to send message")
    }
}

fun getSenderRef(event: PlayerChatEvent): PlayerRef? {
    val fromGetter = tryInvoke(event, "getSender") as? PlayerRef
    if (fromGetter != null) return fromGetter
    return tryGetField(event, "sender") as? PlayerRef
}
