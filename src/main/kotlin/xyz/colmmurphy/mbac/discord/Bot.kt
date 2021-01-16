package xyz.colmmurphy.mbac.discord

import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import xyz.colmmurphy.mbac.discord.GuildMessageListener
import java.util.*
import javax.security.auth.login.LoginException

class Bot: EventListener{
    companion object {
        lateinit var jda: JDA
        @Throws(LoginException::class, InterruptedException::class)
        fun onEnable() {
            jda = JDABuilder.createDefault(dotenv()["API_KEY"])
                .addEventListeners(GuildMessageListener())
                .build()
                .awaitReady()
        }
    }
}