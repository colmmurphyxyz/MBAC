package xyz.colmmurphy.mbac.discord

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import xyz.colmmurphy.mbac.Db
import xyz.colmmurphy.mbac.Player
import xyz.colmmurphy.mbac.enums.values
import java.awt.Color

class GuildMessageListener: ListenerAdapter() {
    var PREFIX = ";"
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.message.contentRaw.startsWith(PREFIX)) return
        if (event.channel.id != values.LOG_CHANNEL.id && event.channel.id != values.CHESS_CHANNEL.id) return
        if (event.author.isBot || event.isWebhookMessage) return
        val cmd = event.message.contentRaw.toLowerCase().substringAfter(PREFIX).split(" ")
        println(cmd.joinToString(", "))
        when (cmd[0]) {
            "testgame" -> {
                val match = ChessGame(event.message.author, event.author, event.channel)
                match.onGameStart()
            }
            "chess" -> {
                val players = event.message.mentionedUsers
                if (players.isNullOrEmpty()) {
                    event.channel.sendMessage("Chess with who?")
                        .queue()
                    return
                } else if (players.contains(event.message.author)) {
                    event.channel.sendMessage("You can't play against yourself")
                        .queue()
                    return
                }
                event.channel.sendMessage(EmbedBuilder()
                    .setTitle("${event.author.asTag}'s game")
                    .setColor(Color.blue)
                    .addField("", "${players[0].asMention}, do you accept ${event.message.author.asMention}'s challenge?", true)
                    .build())
                    .queue { message ->
                        message.addReaction("U+2705").queue() //tick
                        message.addReaction("U+274C").queue() //cross
                    }
                val waiter = Bot.waiter
                waiter.waitForEvent(MessageReactionAddEvent::class.java, {
                    e -> e.member!!.user.equals(players[0]) && e.reaction.reactionEmote.asCodepoints.equals("U+2705")
                }, {
                    e -> e.channel.sendMessage("Starting game").queue()
                    val cg = ChessGame(event.message.author, players[0], event.channel)
                    cg.onGameStart()
                })
            }
            "stats" -> {
                event.channel.sendTyping().queue()
                val p: Player = Db.getPlayerFromId(if (event.message.mentionedUsers.isNullOrEmpty()) {
                    event.message.author.id
                } else event.message.mentionedUsers[0].id)
                val embedBuilder = EmbedBuilder()
                    .setColor(Color.blue)
                    .setTitle("${if (event.message.mentionedUsers.isNullOrEmpty()) {
                        event.message.author.asTag
                    } else {event.message.mentionedUsers[0].asTag}}'s stats:")
                    .addField(" ",
                        "**Elo:** ${p.elo}\n" +
                                "**Games played:** ${p.win + p.lose + p.draw}\n" +
                                "**Games won:** ${p.win}\n" +
                                "**Games lost:** ${p.lose}\n" +
                                "**Games drawn:** ${p.draw}\n" +
                                "**Win/loss ratio:** ${try {
                                    (p.win / p.lose).toString().substring(0, 3)
                                } catch (e: StringIndexOutOfBoundsException) {
                                    (p.win / p.lose).toString()
                                } catch (e: ArithmeticException) {
                                    "N/A"
                                }}\n" +
                                "**Active since:** ${java.time.format.DateTimeFormatter.ISO_INSTANT
                                    .format(java.time.Instant.ofEpochSecond(p.activeSince.toLong()/1000))}",
                        false)
                event.channel.sendMessage(embedBuilder.build())
                    .queue()
            }
        }
    }
}