package de.randombyte.entityparticles.commands

import de.randombyte.entityparticles.EntityParticles.Companion.PLAYER_ARG
import de.randombyte.entityparticles.data.RemoverItemData
import de.randombyte.entityparticles.singleCopy
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.text.serializer.TextSerializers

internal class GiveRemoverItemCommand(
        private val getRemoverItem: () -> ItemStack
) : CommandExecutor {
    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val player = args.getOne<Player>(PLAYER_ARG).get()
        val itemStack = getRemoverItem()
                .singleCopy()
                .apply { offer(RemoverItemData(isRemover = true)) }
        player.inventory.offer(itemStack)
        player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&aGave the remover to ${player.name}!"))
        return CommandResult.success()
    }
}