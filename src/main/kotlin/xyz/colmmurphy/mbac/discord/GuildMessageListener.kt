package xyz.colmmurphy.mbac.discord

import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import xyz.colmmurphy.mbac.enums.values
import java.util.*

class GuildMessageListener: ListenerAdapter() {
    var PREFIX = ";"
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.message.contentRaw.startsWith(PREFIX)) return
        if (event.channel.id != values.LOG_CHANNEL.id) return
        if (event.author.isBot || event.isWebhookMessage) return
        println("message received from ${event.author.name}#${event.author.discriminator}\n" +
            event.message.contentRaw)
        val cmd = event.message.contentRaw.substringAfter(PREFIX).split(" ")
        println(cmd.joinToString(", "))
        when (cmd[0]) {
            "chess" -> {
                val players = event.message.mentionedUsers
                if (players.isNullOrEmpty()) {
                    event.channel.sendMessage("Chess with who?")
                        .queue()
                    return
                }
                if (players.contains(event.message.author)) {
                    event.channel.sendMessage("You can't play against yourself")
                        .queue()
                    return
                }
                GlobalScope.launch {
                    event.channel.sendMessage("${players[0].asMention}, Do you accept ${event.message.author.asMention}'s challenge?")
                        .queue()
                }

                    Thread.sleep(3000)
                    event.channel.addReactionById(event.channel.latestMessageId, "U+2705")
                        .queue()
                    event.channel.addReactionById(event.channel.latestMessageId, "U+274C")
                        .queue()

            }
        }
    }
}