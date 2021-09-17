package de.randombyte.entityparticles.commands

import de.randombyte.entityparticles.config.Config
import de.randombyte.entityparticles.EntityParticles.Companion.PARTICLE_ID_ARG
import de.randombyte.entityparticles.EntityParticles.Companion.PLAYER_ARG
import de.randombyte.entityparticles.data.ParticleData
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.TextSerializers

internal class GiveParticleItemCommand(private val getParticle: (id: String) -> Config.Particle?): CommandExecutor {
    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val player = args.getOne<Player>(PLAYER_ARG).get()
        val particleId = args.getOne<String>(PARTICLE_ID_ARG).get()

        val particle = getParticle(particleId)
                ?: throw CommandException(Text.of("Particle '$particleId' is not available!"))

        val itemStack = particle.createItemStack()
                .apply { offer(ParticleData(id = particleId, isActive = false)) }
        player.inventory.offer(itemStack)
        player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&aGave '$particleId' to ${player.name}!"))
        return CommandResult.success()
    }
}