package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import net.luckperms.api.LuckPermsProvider
import java.util.UUID

const val PERMISSION_NODE = "loyfael.staffchat"

fun readUuid(target: Any): UUID? {
    Log.dbg("readUuid: inspecting target=${target.javaClass.name}")
    val candidates = arrayOf(
        tryInvoke(target, "getUniqueId"),
        tryInvoke(target, "getUuid"),
        tryGetField(target, "uniqueId"),
        tryGetField(target, "uuid"),
    )

    for (value in candidates) {
        Log.dbg("readUuid: candidate -> ${if (value == null) "<null>" else value::class.java.name + ":" + value}")
        when (value) {
            is UUID -> return value
            is String -> {
                try {
                    return UUID.fromString(value)
                } catch (_: IllegalArgumentException) {
                    Log.dbg("readUuid: failed to parse UUID from string='$value'")
                }
            }
        }
    }
    return null
}

fun hasPermissionFallback(target: Any): Boolean {
    val result = (tryInvoke(target, "hasPermission", PERMISSION_NODE) as? Boolean) == true
    Log.dbg("hasPermissionFallback: target=${target.javaClass.name} -> $result")
    return result
}

fun hasPermissionViaLuckPerms(uuid: UUID): Boolean? {
    Log.dbg("hasPermissionViaLuckPerms: searching LuckPerms for uuid=$uuid")
    val api = try {
        LuckPermsProvider.get()
    } catch (t: Throwable) {
        Log.dbg("hasPermissionViaLuckPerms: LuckPermsProvider.get() threw ${t::class.java.name}: ${t.message}")
        return null
    }

    val user = api.userManager.getUser(uuid)
    Log.dbg("hasPermissionViaLuckPerms: user -> ${if (user == null) "<null>" else "present"}")
    if (user == null) return null

    val queryOptions = try {
        api.contextManager.getQueryOptions(user)
            .orElseGet { api.contextManager.staticQueryOptions }
    } catch (t: Throwable) {
        Log.dbg("hasPermissionViaLuckPerms: contextManager.getQueryOptions threw ${t::class.java.name}: ${t.message}")
        return null
    }

    return try {
        val res = user.cachedData
            .getPermissionData(queryOptions)
            .checkPermission(PERMISSION_NODE)
            .asBoolean()
        Log.dbg("hasPermissionViaLuckPerms: checkPermission -> $res")
        res
    } catch (t: Throwable) {
        Log.dbg("hasPermissionViaLuckPerms: permission check threw ${t::class.java.name}: ${t.message}")
        null
    }
}

fun senderHasPermission(event: PlayerChatEvent): Boolean {
    Log.dbg("senderHasPermission: start")
    val senderRef = getSenderRef(event)
    Log.dbg("senderHasPermission: senderRef=${if (senderRef == null) "<null>" else senderRef.username}")

    var senderUuid: UUID? = null
    if (senderRef != null) {
        senderUuid = readUuid(senderRef)
        Log.dbg("senderHasPermission: readUuid(senderRef) -> ${senderUuid ?: "<null>"}")
    }

    if (senderUuid == null) {
        val playerObject = tryInvoke(event, "getPlayer") ?: tryGetField(event, "player")
        Log.dbg("senderHasPermission: playerObject=${if (playerObject == null) "<null>" else playerObject.javaClass.name}")
        if (playerObject != null) {
            senderUuid = readUuid(playerObject)
            Log.dbg("senderHasPermission: readUuid(playerObject) -> ${senderUuid ?: "<null>"}")
        }
    }

    val uuidForLuckPerms = senderUuid
    if (uuidForLuckPerms != null) {
        Log.dbg("senderHasPermission: checking LuckPerms for uuid=$uuidForLuckPerms")
        val lpResult = hasPermissionViaLuckPerms(uuidForLuckPerms)
        Log.dbg("senderHasPermission: hasPermissionViaLuckPerms -> ${lpResult ?: "<null>"}")
        if (lpResult != null) return lpResult
    }

    if (senderRef != null && hasPermissionForRef(senderRef)) {
        Log.dbg("senderHasPermission: hasPermissionForRef(senderRef) -> true")
        return true
    }

    val player = tryInvoke(event, "getPlayer") ?: tryGetField(event, "player")
    Log.dbg("senderHasPermission: fallback player=${if (player == null) "<null>" else player.javaClass.name}")
    if (player != null && hasPermissionFallback(player)) {
        Log.dbg("senderHasPermission: hasPermissionFallback(player) -> true")
        return true
    }

    Log.dbg("senderHasPermission: final -> false")
    return false
}

fun hasPermissionForRef(ref: PlayerRef): Boolean {
    val uuid = readUuid(ref)
    Log.dbg("hasPermissionForRef: readUuid -> ${uuid ?: "<null>"}")
    if (uuid != null) {
        val lpResult = hasPermissionViaLuckPerms(uuid)
        Log.dbg("hasPermissionForRef: hasPermissionViaLuckPerms -> ${lpResult ?: "<null>"}")
        if (lpResult != null) return lpResult
    }
    return hasPermissionFallback(ref)
}
