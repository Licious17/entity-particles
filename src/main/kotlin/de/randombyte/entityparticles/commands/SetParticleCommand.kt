package de.randombyte.entityparticles.commands

import de.randombyte.entityparticles.config.Config
import de.randombyte.entityparticles.EntityParticles
import de.randombyte.entityparticles.EntityParticles.Companion.PARTICLE_ID_ARG
import de.randombyte.entityparticles.data.particleId
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.text.Text
import java.util.*

internal class SetParticleCommand(
        private val getParticleConfig: (id: String) -> Config.Particle?
) : CommandExecutor {
    internal companion object {
        internal const val WORLD_UUID_ARG = "worldUuid"
        internal const val ENTITY_UUID_ARG = "entityUuid"
    }

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val worldUuidString = args.getOne<String>(WORLD_UUID_ARG).get()
        val entityUuidString = args.getOne<String>(ENTITY_UUID_ARG).get()
        val particleId = args.getOne<String>(PARTICLE_ID_ARG).get()

        val world = Sponge.getServer().getWorld(UUID.fromString(worldUuidString)).orElse(null) ?: throw CommandException(Text.of("World '$worldUuidString' is not available!"))
        val uuid = UUID.fromString(entityUuidString)
        val entity = (world.getEntity(uuid).orElse(null) ?: throw CommandException(Text.of("Entity '$entityUuidString' in world '$world' is not available!")))
        if (particleId == "nothing") {
            entity.particleId = null
            entity.offer(Keys.GLOWING, false)

            EntityParticles.INSTANCE.removeTrackedEntity(entity)

            return CommandResult.success()
        }

        val particleConfig = getParticleConfig(particleId) ?: throw CommandException(Text.of("Particle '$particleId' is not available!"))

        entity.particleId = particleId

        if (particleConfig.glowing) {
            entity.offer(Keys.GLOWING, true)
        }

        EntityParticles.INSTANCE.addTrackedEntity(entity)

        return CommandResult.success()
    }
}
