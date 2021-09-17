package de.randombyte.entityparticles.config

import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * To be overwritten to have fields for various [ConfigHolder]s.
 */
abstract class ConfigAccessor(val configPath: Path) {

    init {
        if (Files.notExists(configPath)) {
            Files.createDirectories(configPath)
        }
    }

    abstract val holders: List<ConfigHolder<*>>

    fun reloadAll() {
        holders.forEach(ConfigHolder<*>::reload)
        reloadedAll()
    }

    /**
     * Called when all configs were reloaded.
     */
    open fun reloadedAll() { }

    protected inline fun <reified T : Any> getConfigHolder(configName: String): ConfigHolder<T> {
        return ConfigManager(HoconConfigurationLoader.builder().setPath(configPath.resolve(configName)).build(), T::class.java).toConfigHolder()
    }

}