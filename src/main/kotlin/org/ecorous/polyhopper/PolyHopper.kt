package org.ecorous.polyhopper

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import org.ecorous.polyhopper.config.Config
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.config.QuiltConfig
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.coroutines.runBlocking
import net.minecraft.server.network.ServerPlayerEntity
import org.ecorous.polyhopper.compat.fabrictailor.FabricTailorContextFactory
import org.ecorous.polyhopper.helpers.ChatCommandContext
import org.ecorous.polyhopper.helpers.ChatCommandContextFactory
import org.ecorous.polyhopper.helpers.VanillaContextFactory
import org.quiltmc.loader.api.QuiltLoader
import java.io.File

object PolyHopper : ModInitializer, CoroutineScope {
    const val MODID: String = "polyhopper"
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger("PolyHopper")
    var server: MinecraftServer? = null
    val gson: Gson = Gson()
    var linkedAccounts: LinkedAccounts? = null
    val CONFIG : Config = QuiltConfig.create(MODID, "config", Config::class.java)
    val linkedAccountsPath = File((QuiltLoader.getConfigDir() + File.pathSeparator + MODID + File.pathSeparator + "linked_accounts.json").toString())
    val linkedAccountsJSON = """
        {
            "accounts": []
        }
    """

    override val coroutineContext = Dispatchers.Default

    lateinit var chatCommandContextFactory : ChatCommandContextFactory

    override fun onInitialize(mod: ModContainer) {
        if (CONFIG.bot.accountLinking) {
            if (!linkedAccountsPath.exists()) {
                linkedAccountsPath.writeText(linkedAccountsJSON)
            }

            linkedAccounts = Gson().fromJson(linkedAccountsPath.readText(), LinkedAccounts::class.java)
        }

        chatCommandContextFactory = if (QuiltLoader.isModLoaded("fabrictailor")) {
            FabricTailorContextFactory
        } else {
            VanillaContextFactory
        }

        ServerLifecycleEvents.READY.register {
            server = it

            runBlocking {
                HopperBot.init()
            }

            launch {
                HopperBot.bot.start()
            }

            MessageHooks.onServerStarted()
        }

        ServerLifecycleEvents.STOPPING.register {
            server = null

            MessageHooks.onServerShutdown()

            runBlocking {
                HopperBot.bot.stop()
            }

            if (CONFIG.bot.accountLinking) Utils.writeLinkedAccounts(linkedAccounts!!)
        }
    }

    fun ServerPlayerEntity.getDiscordContext(): ChatCommandContext {
        return chatCommandContextFactory.getContext(this)
    }
}
