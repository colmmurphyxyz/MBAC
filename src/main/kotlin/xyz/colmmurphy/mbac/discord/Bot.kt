package xyz.colmmurphy.mbac.discord

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*
import javax.security.auth.login.LoginException

class Bot: EventListener{
    companion object {
        val waiter = EventWaiter()
        lateinit var jda: JDA
        @Throws(LoginException::class, InterruptedException::class)
        fun onEnable() {
            jda = JDABuilder.createLight(dotenv()["API_KEY"],
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MESSAGE_TYPING,
            GatewayIntent.GUILD_MESSAGE_REACTIONS,)
                .addEventListeners(waiter)
                .addEventListeners(GuildMessageListener())
                .addEventListeners(ReactionListener())
                .build()
                .awaitReady()

        }
    }
}