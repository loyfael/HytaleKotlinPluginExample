package com.loyfael.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.loyfael.exampleplugin.services.KitManager
import com.loyfael.exampleplugin.ui.ItemBrowserUiPage

/**
 * Opens the kit editor UI for a specific kit.
 * Usage: /editkit <name>
 * Permission: loyfael.exampleplugin.command.editkit (auto-generated)
 */
class EditKitCommand : AbstractPlayerCommand("editkit", "exampleplugin.commands.editkit.description") {

    private val kitNameArg: RequiredArg<String> = withRequiredArg(
        "name",
        "exampleplugin.commands.editkit.name.desc",
        ArgTypes.STRING
    )

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        val kitName = kitNameArg.get(context)

        KitManager.getKitAsync(kitName).thenAccept { kit ->
            world.execute {
                if (kit == null) {
                    context.sendMessage(Message.translation("exampleplugin.commands.editkit.notfound")
                        .param("name", kitName))
                    return@execute
                }
                player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kit.name))
            }
        }
    }
}
