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
import org.bson.Document
import org.bson.conversions.Bson
import xyz.colmmurphy.mbac.Db
import xyz.colmmurphy.mbac.Player
import xyz.colmmurphy.mbac.enums.values
import java.awt.Color
import java.util.concurrent.TimeUnit
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCursor
import xyz.colmmurphy.mbac.enums.Strings

class GuildMessageListener: ListenerAdapter() {
    var PREFIX = ";"
    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        if (!e.message.contentRaw.startsWith(PREFIX)) return
        if (e.channel.id != values.LOG_CHANNEL.id && e.channel.id != values.CHESS_CHANNEL.id) return
        if (e.author.isBot || e.isWebhookMessage) return
        val cmd = e.message.contentRaw.toLowerCase().substringAfter(PREFIX).split(" ")
        println(cmd.joinToString(", "))
        when (cmd[0]) {

            "help" -> {
                e.channel.sendMessage(EmbedBuilder()
                    .setTitle("Commands List")
                    .setColor(Color.blue)
                    .addField("commands",
                    "**${PREFIX}help** - displays this menu\n" +
                    "**${PREFIX}chess @<person>** - starts a game with the mentiones user\n" +
                    "**${PREFIX}stats** - shows your statistics\n" +
                    "**${PREFIX}leaderboard** - shows the highest rated users in the server",
                    false)
                    .setFooter("report any issues to Murf#6404")
                    .build())
                    .queue()
            }

            "testgame" -> {
                if (e.message.author.id != "417097416085602315") return
                val match = ChessGame(e.message.author, e.author, e.channel)
                e.channel.sendMessage("Starting test game").queue()
                match.onGameStart()
            }

            "chess" -> {
                val players = e.message.mentionedUsers
                if (players.isNullOrEmpty()) {
                    e.channel.sendMessage("Chess with who?")
                        .queue()
                    return
                } else if (players.contains(e.message.author)) {
                    e.channel.sendMessage("You can't play against yourself")
                        .queue()
                    return
                }
                e.channel.sendMessage(EmbedBuilder()
                    .setTitle("${e.author.asTag}'s game")
                    .setColor(Color.blue)
                    .addField("", "${players[0].asMention}, do you accept ${e.message.author.asMention}'s challenge?", true)
                    .build())
                    .queue { message ->
                        message.addReaction("U+2705").queue() //tick
                        message.addReaction("U+274C").queue() //cross
                    }
                val waiter = Bot.waiter
                waiter.waitForEvent(MessageReactionAddEvent::class.java, {
                    rae -> e.member!!.user.equals(players[0]) && rae.reaction.reactionEmote.asCodepoints.equals("U+2705")
                }, {
                    rae -> rae.channel.sendMessage("Starting game").queue()
                    val cg = ChessGame(e.message.author, players[0], e.channel)
                    cg.onGameStart()
                },
                120L, TimeUnit.SECONDS,
                { ->
                    println("game declined")
                }
                )
            }

            "stats" -> {
                e.channel.sendTyping().queue()
                val p: Player = Db.getPlayerFromId(if (e.message.mentionedUsers.isNullOrEmpty()) {
                    e.message.author.id
                } else e.message.mentionedUsers[0].id)
                val embedBuilder = EmbedBuilder()
                    .setColor(Color.blue)
                    .setTitle("${if (e.message.mentionedUsers.isNullOrEmpty()) {
                        e.message.author.asTag
                    } else {e.message.mentionedUsers[0].asTag}}'s stats:")
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
                e.channel.sendMessage(embedBuilder.build())
                    .queue()
            }

            "leaderboard" -> {
                e.channel.sendTyping().queue()
                val top10 = Db.players.find().sort(BasicDBObject("elo", -1)).limit(5)
                e.guild.loadMembers()
                println(e.guild.memberCache.joinToString(", "))
                val messageList = e.guild.members
                println(messageList.joinToString(", "))
                val iterator: MongoCursor<Document> = top10.iterator()
                var messageBody = ""
                while(iterator.hasNext()) {
                    val iteratorNext = iterator.next().toJson()
                    messageBody += "${
                        try {
                            e.guild.getMemberById(
                                iteratorNext
                                    .substringAfterLast("id\": \"")
                                    .substringBefore("\"")
                            )!!.user.asTag
                        } catch (e: NullPointerException) {
                            "NullPointerException"
                        }
                    } - ${
                        iteratorNext
                            .substringAfter("elo\": ")
                            .substringBefore(",")}\n"
                }
                e.channel.sendMessage(EmbedBuilder()
                    .setTitle("Leaderboard")
                    .setColor(Color.blue)
                    .addField(" ",
                    messageBody,
                    false)
                    .setFooter(Strings.genericEmbedFooter.content)
                    .build())
                    .queue()
            }
        }
    }
}