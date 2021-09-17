package de.randombyte.entityparticles.config

import com.flowpowered.math.vector.Vector3d
import com.flowpowered.math.vector.Vector3i
import de.randombyte.entityparticles.config.Config.Particle.Effect
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.effect.particle.ParticleType
import org.spongepowered.api.effect.particle.ParticleTypes
import org.spongepowered.api.entity.EntityType
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.enchantment.Enchantment
import org.spongepowered.api.item.enchantment.EnchantmentTypes
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.text.serializer.TextSerializers

@ConfigSerializable
internal data class Config(
    @Setting("remover-item", comment = "Ignore the 'glowing' and 'effects' setting.") val removerItem: Particle = Particle(),
    @Setting("particles") val particles: Map<String, Particle> = emptyMap(),
    @Setting("blocked-entities") val blockedEntities: List<EntityType> = emptyList()
) {
    @ConfigSerializable
    internal data class Particle(
            @Setting("item") val item: String = "",
            @Setting("display-name") val displayName: String = "",
            @Setting("item-description") val itemDescription: String = "",
            @Setting("item-enchanted") val itemEnchanted: Boolean = false,
            @Setting("glowing") val glowing: Boolean = false,
            @Setting("effects") val effects: List<Effect> = emptyList()
    ) {
        @ConfigSerializable
        internal data class Effect(
                @Setting("type") val type: ParticleType = ParticleTypes.HEART,
                @Setting("quantity") val quantity: Int = -1,
                @Setting("velocity") val velocity: Vector3d = Vector3d.ONE.negate(),
                @Setting("offset") val offset: Vector3d = Vector3d.ONE.negate(),
                @Setting("center-offset") val centerOffset: Vector3d = Vector3d.ZERO,
                @Setting("interval", comment = "In ticks(20 ticks = 1 second)") val interval: Int = -1,
                @Setting("color", comment = "Supported by redstone dust, mobspell and ambient mobspell") val color: Vector3i = Vector3i.ONE.negate()
        )

        fun createItemStack(): ItemStack = ItemStack.builder()
                .fromSnapshot(findItem())
                .quantity(1) // force single item
                .apply {
                    if (itemEnchanted) {
                        add(Keys.ITEM_ENCHANTMENTS, listOf(Enchantment.of(EnchantmentTypes.LUCK_OF_THE_SEA, 0)))
                        add(Keys.HIDE_ENCHANTMENTS, true)
                    }
                }
                .add(Keys.DISPLAY_NAME, TextSerializers.FORMATTING_CODE.deserialize(displayName))
                .add(Keys.ITEM_LORE, listOf(TextSerializers.FORMATTING_CODE.deserialize(itemDescription)))
                .build()

        private fun findItem(): ItemStackSnapshot {
            val itemType = Sponge.getRegistry().getType(ItemType::class.java, item)
                .orElseThrow { IllegalArgumentException("Couldn't find ItemType for $item") }
            return ItemStack.of(itemType, 1).createSnapshot()
        }
    }

    constructor() : this(
            blockedEntities = listOf(EntityTypes.PLAYER),
            removerItem = Particle(
                    item = "minecraft:bone",
                    displayName = "Particles remover",
                    itemDescription = "",
                    itemEnchanted = true
            ),
            particles = mapOf(
            "love" to Particle(
                    item = "minecraft:blaze_rod",
                    displayName = "&cLove",
                    itemDescription = "Right click an entity to apply this effect",
                    itemEnchanted = true,
                    glowing = false,
                    effects = listOf(Effect(
                            type = ParticleTypes.HEART,
                            quantity = 10,
                            velocity = Vector3d(0.0, 0.3, 0.0),
                            offset = Vector3d.ONE,
                            interval = 20,
                            color = Vector3i.ONE
                    )))
    ))
}