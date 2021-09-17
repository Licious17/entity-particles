package de.randombyte.entityparticles.commands

import de.randombyte.entityparticles.config.Config
import de.randombyte.entityparticles.EntityParticles
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.TextSerializers

internal class NewParticleConfigCommand(
    private val addNewConfig: (id: String, Config.Particle) -> Unit,
    private val updateCommands: () -> Unit
) : CommandExecutor {

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        if (src !is Player)
            throw CommandException(Text.of("Command must be executed by a player"))
        val newId = args.getOne<String>(EntityParticles.PARTICLE_ID_ARG).get()
        addNewConfig(
            newId, Config().particles.getValue("love").copy(
                item = "minecraft:stick"
            )
        )
        src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&aAdded to config!"))
        updateCommands()

        return CommandResult.success()
    }
}