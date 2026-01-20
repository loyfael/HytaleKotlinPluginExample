package com.loyfael.exampleplugin

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.loyfael.exampleplugin.commands.BackCommand
import com.loyfael.exampleplugin.commands.ClearInventoryCommand
import com.loyfael.exampleplugin.commands.EditKitCommand
import com.loyfael.exampleplugin.commands.EditKitsCommand
import com.loyfael.exampleplugin.commands.ExampleCommand
import com.loyfael.exampleplugin.commands.FlyCommand
import com.loyfael.exampleplugin.commands.GodCommand
import com.loyfael.exampleplugin.commands.InvSeeCommand
import com.loyfael.exampleplugin.commands.KitCommand
import com.loyfael.exampleplugin.commands.RepairCommand
import com.loyfael.exampleplugin.commands.UiCommand
import com.loyfael.exampleplugin.commands.SuicideCommand
import com.loyfael.exampleplugin.commands.WaypointCommand
import com.loyfael.exampleplugin.commands.WaypointsCommand
import com.loyfael.exampleplugin.listeners.ArmorHudUpdateSystem
import com.loyfael.exampleplugin.listeners.ChatFormatListener
import com.loyfael.exampleplugin.listeners.FlyFallDamageFilterSystem
 
import com.loyfael.exampleplugin.listeners.GodModeListener
import com.loyfael.exampleplugin.listeners.MapTeleportFeature
import com.loyfael.exampleplugin.listeners.PlayerConnectionListener
import com.loyfael.exampleplugin.listeners.PlayerDeathPositionSystem
import com.loyfael.exampleplugin.listeners.WaypointMapMarkerProvider
import com.loyfael.exampleplugin.services.ArmorHudManager
import com.loyfael.exampleplugin.services.DatabaseManager
import com.loyfael.exampleplugin.services.DeathPositionManager
import com.loyfael.exampleplugin.services.KitManager
import com.loyfael.exampleplugin.services.SessionManager
import com.loyfael.exampleplugin.services.UpdateChecker
import com.loyfael.exampleplugin.services.WaypointManager

class ExamplePlugin(init: JavaPluginInit) : JavaPlugin(init) {
    override fun setup() {
        // Initialize services
        DatabaseManager.initialize(dataDirectory)
        KitManager.initialize(dataDirectory)
        WaypointManager.initialize(dataDirectory)

        // Register commands
        this.commandRegistry.registerCommand(ExampleCommand())
        this.commandRegistry.registerCommand(UiCommand())
        this.commandRegistry.registerCommand(FlyCommand())
        this.commandRegistry.registerCommand(GodCommand())
        this.commandRegistry.registerCommand(InvSeeCommand())
        this.commandRegistry.registerCommand(KitCommand())
        this.commandRegistry.registerCommand(EditKitCommand())
        this.commandRegistry.registerCommand(EditKitsCommand())
        this.commandRegistry.registerCommand(BackCommand())
        this.commandRegistry.registerCommand(ClearInventoryCommand())
        this.commandRegistry.registerCommand(RepairCommand())
        this.commandRegistry.registerCommand(SuicideCommand())
        this.commandRegistry.registerCommand(WaypointCommand())
        this.commandRegistry.registerCommand(WaypointsCommand())

        // Register ECS systems
        this.entityStoreRegistry.registerSystem(FlyFallDamageFilterSystem())
        this.entityStoreRegistry.registerSystem(PlayerDeathPositionSystem())
        this.entityStoreRegistry.registerSystem(ArmorHudUpdateSystem())

        // Register map marker providers for all worlds
        this.eventRegistry.registerGlobal(AddWorldEvent::class.java) { event ->
            val worldMapManager = event.getWorld().worldMapManager
            worldMapManager.addMarkerProvider("waypoints", WaypointMapMarkerProvider.INSTANCE)
            worldMapManager.addMarkerProvider("mapteleport", MapTeleportFeature.INSTANCE)
        }

        // Handle player disconnect
        this.eventRegistry.registerGlobal(PlayerDisconnectEvent::class.java) { event ->
            MapTeleportFeature.clearPlayer(event.playerRef.getUuid())
        }

        // Handle player ready in world
        this.eventRegistry.registerGlobal(PlayerReadyEvent::class.java) { event ->
            GodModeListener.onPlayerReady(event)
            PlayerConnectionListener.onPlayerReady(event)
        }
        

        // Format chat messages differently for OPs and regular players
        this.eventRegistry.registerGlobal(PlayerChatEvent::class.java) { event ->
            ChatFormatListener.onPlayerChat(event)
        }
    }

    override fun shutdown() {
        // Clear login sessions
        SessionManager.clear()

        // Clear death positions
        DeathPositionManager.clear()

        // Clear armor HUD states
        ArmorHudManager.clear()

        // Clear map teleport tracking
        MapTeleportFeature.clearAll()

        // Close database connection
        DatabaseManager.shutdown()

        // Shutdown waypoint manager
        WaypointManager.shutdown()

        // Shutdown update checker
        UpdateChecker.shutdown()
    }
}
