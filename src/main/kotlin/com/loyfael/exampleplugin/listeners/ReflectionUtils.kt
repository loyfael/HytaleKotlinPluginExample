package com.loyfael.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message

fun tryInvoke(target: Any, methodName: String, vararg args: Any?): Any? {
    return try {
        val candidates = target.javaClass.methods.filter { m ->
            m.name == methodName && m.parameterTypes.size == args.size
        }
        for (method in candidates) {
            try {
                val mappedArgs = Array<Any?>(args.size) { idx ->
                    val expected = method.parameterTypes[idx]
                    val arg = args[idx]
                    if (arg == null) return@Array null
                    // exact or assignable
                    if (expected.isAssignableFrom(arg.javaClass)) return@Array arg
                    // common conversions
                    if (expected == String::class.java) return@Array arg.toString()
                    if (expected.isAssignableFrom(Message::class.java)) return@Array when (arg) {
                        is Message -> arg
                        else -> Message.raw(arg.toString())
                    }
                    // fallback: try to pass as-is (may fail for primitives)
                    return@Array arg
                }
                return method.invoke(target, *mappedArgs)
            } catch (t: Throwable) {
                // try next candidate
                Log.dbg("tryInvoke (candidate): ${t::class.java.name} when invoking ${method.name} on ${target.javaClass.name}: ${t.message}")
            }
        }
        null
    } catch (t: Throwable) {
        Log.dbg("tryInvoke: ${t::class.java.name} when invoking $methodName on ${target.javaClass.name}: ${t.message}")
        null
    }
}

fun tryGetField(target: Any, fieldName: String): Any? {
    return try {
        val publicField = target.javaClass.fields.firstOrNull { it.name == fieldName }
        if (publicField != null) return publicField.get(target)

        val declaredField = target.javaClass.declaredFields.firstOrNull { it.name == fieldName }
            ?: return null
        declaredField.isAccessible = true
        declaredField.get(target)
    } catch (t: Throwable) {
        Log.dbg("tryGetField: ${t::class.java.name} when getting field '$fieldName' on ${target.javaClass.name}: ${t.message}")
        null
    }
}

fun trySetField(target: Any, fieldName: String, value: Any?) {
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
    } catch (t: Throwable) {
        Log.dbg("trySetField: ${t::class.java.name} when setting field '$fieldName' on ${target.javaClass.name}: ${t.message}")
    }
}
