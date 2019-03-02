package de.randombyte.entityparticles

import com.flowpowered.math.vector.Vector3i
import com.google.inject.Inject
import de.randombyte.entityparticles.commands.GiveParticleItemCommand
import de.randombyte.entityparticles.commands.GiveRemoverItemCommand
import de.randombyte.entityparticles.commands.NewParticleConfigCommand
import de.randombyte.entityparticles.commands.SetParticleCommand
import de.randombyte.entityparticles.commands.SetParticleCommand.Companion.ENTITY_UUID_ARG
import de.randombyte.entityparticles.commands.SetParticleCommand.Companion.WORLD_UUID_ARG
import de.randombyte.entityparticles.data.EntityParticlesKeys
import de.randombyte.entityparticles.data.EntityParticlesKeys.IS_REMOVER
import de.randombyte.entityparticles.data.EntityParticlesKeys.PARTICLE_ID
import de.randombyte.entityparticles.data.ParticleData
import de.randombyte.entityparticles.data.RemoverItemData
import de.randombyte.entityparticles.data.particleId
import de.randombyte.kosp.config.ConfigManager
import de.randombyte.kosp.extensions.executeAsConsole
import de.randombyte.kosp.extensions.orNull
import de.randombyte.kosp.extensions.red
import de.randombyte.kosp.extensions.toText
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.bstats.sponge.Metrics
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.command.args.GenericArguments.*
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.data.DataRegistration
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.effect.particle.ParticleEffect
import org.spongepowered.api.effect.particle.ParticleOptions
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Cancellable
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.entity.DestructEntityEvent
import org.spongepowered.api.event.entity.InteractEntityEvent
import org.spongepowered.api.event.entity.SpawnEntityEvent
import org.spongepowered.api.event.filter.Getter
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.event.game.state.GameStartingServerEvent
import org.spongepowered.api.event.item.inventory.UseItemStackEvent
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.Dependency
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.Task
import org.spongepowered.api.util.Color
import java.util.*

@Plugin(id = EntityParticles.ID,
        name = EntityParticles.NAME,
        version = EntityParticles.VERSION,
        dependencies = [(Dependency(id = "byte-items", optional = true))],
        authors = [(EntityParticles.AUTHOR)])
class EntityParticles @Inject constructor(
        private val logger: Logger,
        @DefaultConfig(sharedRoot = true) configLoader: ConfigurationLoader<CommentedConfigurationNode>,
        private val pluginContainer: PluginContainer,
        val bStats: Metrics
) {
    internal companion object {
        const val ID = "entity-particles"
        const val NAME = "EntityParticles"
        const val VERSION = "3.1.1"
        const val AUTHOR = "RandomByte"

        const val ROOT_PERMISSION = ID

        const val PARTICLE_ID_ARG = "particleId"
        const val PLAYER_ARG = "player"

        const val PIXELMON_ID = "pixelmon"
        const val PIXELMON_PARTICLE_TAG_KEY = "$ID:particle"

        private val _INSTANCE = lazy { Sponge.getPluginManager().getPlugin(EntityParticles.ID).get().instance.get() as EntityParticles }
        val INSTANCE: EntityParticles get() = _INSTANCE.value
    }

    private val configManager = ConfigManager(
            configLoader = configLoader,
            clazz = Config::class.java
    )

    private lateinit var config: Config

    // <world, <entity, particleId>>
    val trackedEntities: MutableMap<UUID, MutableMap<UUID, String>> = mutableMapOf()

    @Listener
    fun onPreInit(event: GamePreInitializationEvent) {
        EntityParticlesKeys.buildKeys()

        Sponge.getDataManager().registerLegacyManipulatorIds("de.randombyte.entityparticles.data.ParticleData", DataRegistration.builder()
                .dataClass(ParticleData::class.java)
                .immutableClass(ParticleData.Immutable::class.java)
                .builder(ParticleData.Builder())
                .manipulatorId("particle")
                .dataName("Particle")
                .buildAndRegister(pluginContainer))

        Sponge.getDataManager().registerLegacyManipulatorIds("de.randombyte.entityparticles.data.RemoverItemData", DataRegistration.builder()
                .dataClass(RemoverItemData::class.java)
                .immutableClass(RemoverItemData.Immutable::class.java)
                .builder(RemoverItemData.Builder())
                .manipulatorId("remover")
                .dataName("Remover")
                .buildAndRegister(pluginContainer))
    }

    /**
     * All the config stuff(generate, commands and the task) has to be this late to let
     * Sponge load all the DataManipulators.
     */
    @Listener
    fun onGameLoadComplete(event: GameLoadCompleteEvent) {
        loadConfig()
        registerCommands()

        logger.info("$NAME loaded: $VERSION")
    }

    @Listener
    fun onReload(event: GameReloadEvent) {
        loadConfig()
        registerCommands()

        logger.info("Reloaded!")
    }

    @Listener
    fun onServerStarting(event: GameStartingServerEvent) {
        startParticleTask()
    }

    @Listener
    fun onLoadEntity(event: SpawnEntityEvent) {
        event.entities
                .filter { it.particleId != null }
                .forEach { addTrackedEntity(it) }
    }

    @Listener
    fun onUnloadEntity(event: DestructEntityEvent) {
        removeTrackedEntity(event.targetEntity)
    }

    fun addTrackedEntity(entity: Entity) {
        val trackedEntities = trackedEntities.getOrPut(entity.location.extent.uniqueId) { mutableMapOf() }
        trackedEntities += entity.uniqueId to entity.particleId!!
    }

    fun removeTrackedEntity(entity: Entity) {
        trackedEntities[entity.location.extent.uniqueId]?.remove(entity.uniqueId)
    }

    private fun loadConfig() {
        config = configManager.load()
        saveConfig() // generate config
    }

    private fun saveConfig() {
        configManager.save(config)
    }

    @Listener
    fun onRightClickEntity(event: InteractEntityEvent.Secondary.MainHand, @First player: Player, @Getter("getTargetEntity") targetEntity: Entity) {
        if (targetEntity.type in config.blockedEntities) return
        val itemInHand = player.getItemInHand(HandTypes.MAIN_HAND).orNull() ?: return

        val particleId = itemInHand.get(PARTICLE_ID).orNull()
        val isRemover = itemInHand.get(IS_REMOVER).orElse(false)

        when {
            particleId != null -> {
                player.setItemInHand(HandTypes.MAIN_HAND, itemInHand.setAmount(itemInHand.quantity - 1))
                ("entityParticles set ${targetEntity.location.extent.uniqueId} ${targetEntity.uniqueId} $particleId").executeAsConsole()
            }
            isRemover -> {
                if (targetEntity.particleId == null) return
                player.setItemInHand(HandTypes.MAIN_HAND, itemInHand.setAmount(itemInHand.quantity - 1))
                ("entityParticles set ${targetEntity.location.extent.uniqueId} ${targetEntity.uniqueId} nothing").executeAsConsole()
            }
            else -> return // nothing, no EntityParticle item, prevent cancelling the event
        }

        event.isCancelled = true
    }

    @Listener
    fun onPlaceParticleItem(event: InteractBlockEvent.Secondary.MainHand, @First player: Player) {
        if (event.targetBlock.state.type != BlockTypes.AIR) onUseItem(event, player)
    }

    @Listener
    fun onUseItemEvent(event: UseItemStackEvent.Start, @First player: Player) = onUseItem(event, player)

    private fun onUseItem(event: Cancellable, player: Player) {
        val item = player.getItemInHand(HandTypes.MAIN_HAND).orNull() ?: return
        if (item.get(EntityParticlesKeys.PARTICLE_ID).isPresent || item.get(EntityParticlesKeys.IS_REMOVER).isPresent) {
            event.isCancelled = true
            player.sendMessage("You can't use a ParticleItem!".red())
        }
    }

    private fun registerCommands() {
        Sponge.getCommandManager().getOwnedBy(this).forEach { Sponge.getCommandManager().removeMapping(it) }

        val particleIdChoices = config.particles.keys.map { it to it }.toMap()

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.give")
                        .arguments(
                                playerOrSource(PLAYER_ARG.toText()),
                                choices(PARTICLE_ID_ARG.toText(), particleIdChoices))
                        .executor(GiveParticleItemCommand(getParticle = { id -> config.particles[id] }))
                        .build(), "give")
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.set")
                        .arguments(
                                string(WORLD_UUID_ARG.toText()),
                                string(ENTITY_UUID_ARG.toText()),
                                choices(PARTICLE_ID_ARG.toText(), particleIdChoices.plus("nothing" to "nothing")))
                        .executor(SetParticleCommand(getParticleConfig = { id -> config.particles[id] }))
                        .build(), "set")
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.new-config")
                        .arguments(string(PARTICLE_ID_ARG.toText()))
                        .executor(NewParticleConfigCommand(
                                addNewConfig = { id, particle ->
                                    config = config.copy(particles = config.particles + (id to particle))
                                    saveConfig()
                                },
                                updateCommands = { registerCommands() }
                        ))
                        .build(), "newconfig")
                .child(CommandSpec.builder()
                        .child(CommandSpec.builder()
                                .permission("$ROOT_PERMISSION.remover-item.give")
                                .arguments(playerOrSource(PLAYER_ARG.toText()))
                                .executor(GiveRemoverItemCommand(getRemoverItem = { config.removerItem.createItemStack() }))
                                .build(), "give")
                        .build(), "removeritem")
                .build(), "entityparticles", "particles", "ep")
    }

    private fun startParticleTask() {
        Task.builder()
                .intervalTicks(1)
                .execute { ->
                    if (!Sponge.isServerAvailable()) {
                        return@execute
                    }

                    Sponge.getServer().worlds.forEach worldLoop@ { world ->
                        (trackedEntities[world.uniqueId] ?: return@worldLoop)
                                .mapNotNull { (uuid, id) ->
                                    val entity = world.getEntity(uuid).orNull()
                                            ?: return@mapNotNull null
                                    entity to id
                                }
                                .forEach entityLoop@ { (entity, id) ->
                                    val particleConfig = config.particles[id]
                                    if (particleConfig == null) {
                                        entity.particleId = null
                                        removeTrackedEntity(entity)

                                        return@entityLoop
                                    }
                                    particleConfig.effects.forEach { effect ->
                                        val doEffectThisTick = Sponge.getServer().runningTimeTicks % effect.interval == 0
                                        if (doEffectThisTick) {
                                            entity.spawnParticles(effect)
                                        }
                                    }
                                }
                    }
                }
                .submit(this)
    }

    private fun Entity.spawnParticles(effect: Config.Particle.Effect) {
        val particleEffect = ParticleEffect.builder()
                .type(effect.type)
                .quantity(effect.quantity)
                .velocity(effect.velocity)
                .offset(effect.offset)
                .option(ParticleOptions.COLOR, Color.of(effect.color.coerceIn(Vector3i.ZERO..Vector3i.from(255, 255, 255))))
                .build()
        location.extent.spawnParticles(particleEffect, location.position.add(effect.centerOffset))
    }

    private fun ItemStack.setAmount(amount: Int): ItemStack = apply { quantity = amount }
}