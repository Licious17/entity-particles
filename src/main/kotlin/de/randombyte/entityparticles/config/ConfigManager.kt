package de.randombyte.entityparticles.config

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader

@Suppress("UnstableApiUsage")
class ConfigManager <T : Any> (private val configLoader: ConfigurationLoader<CommentedConfigurationNode>, clazz: Class<T>) {

    private val typeToken: TypeToken<T> = TypeToken.of(clazz)
    private val options: ConfigurationOptions = ConfigurationOptions.defaults()
        .withShouldCopyDefaults(true)

    /**
     * Returns the saved config. If none exists a new one is generated and already saved.
     */
    @Suppress("UNCHECKED_CAST")
    fun load(): T = configLoader.load(options).getValue(typeToken) ?: run {
        save(typeToken.rawType.newInstance() as T)
        load()
    }

    @Deprecated("Use load() instead", ReplaceWith("load()"))
    fun get(): T = load()

    fun save(config: T) = configLoader.apply { save(load(options).setValue(typeToken, config)) }

    /**
     * get() already generates the config when none exists but this method also inserts missing nodes
     * and reformats the structure.
     */
    fun generate() = save(load())

}