package xyz.colmmurphy.mbac.discord

import com.mongodb.BasicDBObject
import de.swirtz.ktsrunner.objectloader.KtsObjectLoader
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import xyz.colmmurphy.mbac.Db
import xyz.colmmurphy.mbac.commands.Commands
import xyz.colmmurphy.mbac.commands.Commands.*
import xyz.colmmurphy.mbac.commands.Eval
import xyz.colmmurphy.mbac.enums.Strings
import xyz.colmmurphy.mbac.enums.values
import java.awt.Color
import java.io.File
import java.io.PrintStream
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CommandListener : ListenerAdapter() {
    var PREFIX = ";"
    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        if (!e.message.contentRaw.startsWith(PREFIX)
            || e.author.isBot
            || e.isWebhookMessage
        ) { return }
        if (e.channel.id != values.CHESS_CHANNEL.id && e.channel.id != values.LOG_CHANNEL.id) {
            println("Message was not in correct channel")
            e.channel.sendMessage("I only listen to messages in #chess").queue()
            return
        }

        val msg = e.message.contentRaw.substringAfter(PREFIX).toLowerCase().split(" ")

        // finds the Command enum that the alias belongs to
        val command: Commands? = Commands.belongsTo(msg[0])
        if (command == null) {
            e.channel.sendMessage("Couldn't find command **${msg[0]}**").queue()
            return
        } else {
            if (command.ownerOnly && e.author.id != values.OWNER.id) {
                e.channel.sendMessage("You can't use that command").queue()
                return
            }
            when (command) {
                EVAL -> {
                    val input = e.message.contentRaw.substringAfter("```").substringBeforeLast("```")
                    val eval = Eval(e, input)
                    eval.writeToFile()
                    var toBeRun = "```"
                    val fileLines = File("src/main/resources/toEvaluate.kts").bufferedReader().readLines()
                    for (i in fileLines) {
                        toBeRun += "$i \n"
                    }
                    e.channel.sendMessage(EmbedBuilder()
                        .setTitle("Code to be evaluated")
                        .addField(" ",
                        "$toBeRun ```",
                        false)
                        .build()
                    ).queue { message -> message.addReaction("U+2705").queue()} //adds a tick mark reaction
                    val waiter = Bot.waiter
                    waiter.waitForEvent(MessageReactionAddEvent::class.java, {
                            rae -> (rae.member!!.user == e.author) && rae.reaction.reactionEmote.asCodepoints.equals("U+2705")
                    }, {
                        e.channel.sendMessage("Executing code...").queue()
                        eval.runCode()
                        val output = eval.readOutput()
                        var outputMessage = "```\n"
                        for (i in output) {
                            outputMessage += "$ $i \n"
                        }
                        try {
                            e.channel.sendMessage("$outputMessage```").queue()
                        } catch (IAE: IllegalArgumentException) {
                            e.channel.sendMessage("output is too long to send as message").queue()
                        }
                        e.channel.sendFile(File("src/main/resources/output.txt")).queue()
                    }, 60L, TimeUnit.SECONDS,
                        { ->
                            e.channel.sendMessage("timed out").queue()
                        }
                    )
                }
                HELP -> {
                    e.channel.sendMessage(
                        EmbedBuilder()
                            .setTitle("Commands:")
                            .setColor(Color.blue)
                            .addField(" ",
                                Commands.cmdList(), // String containing all commands in Commands.kt
                                true
                            )
                            .setFooter(Strings.genericEmbedFooter.content)
                            .build()
                    ).queue()
                }

                LEADERBOARD -> {
                    var n = 5
                    if (msg.size >= 2) {
                        try {
                            n = if (msg[1].toInt() <= 20) msg[1].toInt() else 20
                        } catch (e: NumberFormatException) {
                            n = 5
                        }
                    }
                    val topPlayers = Db.players.find().sort(BasicDBObject("elo", -1)).limit(n)
                    val topPlayersIterator = topPlayers.iterator()
                    var messageBody = ""
                    var k = 1
                    while (topPlayersIterator.hasNext()) {
                        val p = topPlayersIterator.next().toJson()
                        val pName = try {
                            e.guild.getMemberById(
                                p.substringAfterLast("id\": \"")
                                 .substringBefore("\"")
                            )!!.user.asTag
                        } catch (e: NullPointerException) {
                            "NullPointerException"
                        }
                        val pElo = p.substringAfterLast("elo\": ")
                                    .substringBefore(",")
                        messageBody += "$k - $pName - $pElo \n"
                        k++
                    }
                    e.channel.sendMessage(EmbedBuilder()
                        .setTitle("Leaderboard")
                        .setColor(Color.blue)
                        .addField("",
                        messageBody,
                        true)
                        .setFooter(Strings.genericEmbedFooter.content)
                        .build()
                    ).queue()
                }

                STATS -> TODO()

                else -> TODO()
            }
        }
    }
}