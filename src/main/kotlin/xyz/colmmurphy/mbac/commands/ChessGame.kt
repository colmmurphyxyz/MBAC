package xyz.colmmurphy.mbac.commands

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import xyz.colmmurphy.mbac.Db
import xyz.colmmurphy.mbac.discord.Bot
import xyz.colmmurphy.mbac.enums.Outcomes
import xyz.colmmurphy.mbac.enums.Strings
import java.awt.Color
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.round

class ChessGame(val host: User, val guest: User, val tc: TextChannel) {
    /**
     * this class should:
     * wait for result of the game
     * calculate elo gain/loss for both players
     * make changes to the DB accordingly
     */
    private val k = 32 //the K-factor for calculating Elo, typically set to 32 for most players, and 26 for high-ranked players
    private val propFactor = 400 //This means that if a players Elo is 400 higher than their opponent's, they will be 10 times
                        //more likely to win

    private lateinit var waiter: EventWaiter;

    private val hostPlayer = Db.getPlayerFromId(host.id)
    private val guestPlayer = Db.getPlayerFromId(guest.id)

    // the expected outcome of the host, i.e the probability that the host will win
    private val E_h = 1 / (1 + 10.0.pow((guestPlayer.elo - hostPlayer.elo).div(propFactor.toDouble())))
    private val E_g = 1 / (1 + 10.0.pow((hostPlayer.elo - guestPlayer.elo).div(propFactor.toDouble())))

    fun onGameStart() {
        println("${this.host.name} started a game with ${this.guest.name}")
        var lmid = "foo"
        lateinit var msg: Message
        tc.sendMessage(
            EmbedBuilder()
                .setTitle("${host.name} vs. ${guest.name}")
                .setColor(Color.blue)
                .addField(" ", "__Both players__ must react to this message with the winner\n" +
                        ":one: - ${host.name}\n" +
                        ":two: - ${guest.name}\n" +
                        ":three: - draw",
                    true)
            .build())
            .queue { message ->
                message.addReaction("U+31U+fe0fU+20e3").queue() //:one:
                message.addReaction("U+32U+fe0fU+20e3").queue() //:two:
                message.addReaction("U+33U+fe0fU+20e3").queue() //:three:
                lmid = message.id
                msg = message
                println(lmid)
                println(msg.author)
            }
        Thread.sleep(2000)
        val waiter = Bot.waiter
        println("created EventWaiter")
        waiter.waitForEvent(GuildMessageReactionAddEvent::class.java,
            { e ->
                (e.user.equals(host) || e.user.equals(guest))
                        && e.messageId.equals(lmid)
                        && voteWinner(e)
            },
            { e ->
                // there should be no need for this message now but i'm keeping the code just in case
//                e.channel.sendMessage(EmbedBuilder()
//                    .setTitle("Game is finished")
//                    .setThumbnail(if (votes[0] == Outcomes.hostWin) {
//                        host.avatarUrl
//                    } else guest.avatarUrl)
//                    .setColor(Color.blue)
//                    .addField(" ",
//                    if (votes[0] == Outcomes.hostWin) {
//                        "${host.name} has won"
//                    } else if (votes[0] == Outcomes.guestWin) {
//                        "${guest.name} has won"
//                    } else "Game ended in a draw",
//                    false)
//                    .build())
//                    .queue()
                endOfGameCalcs()
            },
            1800L, TimeUnit.SECONDS,
            { ->
                tc.sendMessage("${host.asMention}, your game has timed out, start a new game with " +
                    "```;chess @<Person>``` if you want to record your result").queue()
            }
        )

    }

    private var hostWinVotes = 0
    private var guestWinVotes = 0
    private var drawVotes = 0
    private var votes: Array<Outcomes?> = arrayOf(null, null)

    /**
     * returns true if both players have voted for the same person as winner
     */
    private fun voteWinner(e: GuildMessageReactionAddEvent): Boolean {
        val person = if (e.user.equals(host)) 0 else 1
        when(e.reactionEmote.asCodepoints) {
            "U+31U+fe0fU+20e3" -> { // :one:
                votes[person] = Outcomes.hostWin
            }
            "U+32U+fe0fU+20e3" -> { // :two:
                votes[person] = Outcomes.guestWin
            }
            "U+33U+fe0fU+20e3" -> { // :three
                votes[person] = Outcomes.draw
            }
        }
        return votes[0] == votes[1]
    }

    /**
     * updates the db with new wlo scores, and messages the channel notifying the users
     */
    private fun endOfGameCalcs() {
        val s: Double = if (votes[0] == Outcomes.hostWin) {
            1.0
        } else if (votes[0] == Outcomes.guestWin) {
            0.0
        } else 0.5
        val hostEloChange = round(k * (s - E_h)).toInt()
        val guestEloChange = ceil(k * ((1 - s) - E_g)).toInt()

        if (s == 0.5) {
            Db.updatePlayer(host.id, eloChange=hostEloChange, drawChange=1)
            Db.updatePlayer(guest.id, eloChange=(hostEloChange * -1), drawChange=1)
        } else {
            Db.updatePlayer(host.id,
                eloChange=hostEloChange,
                winChange=s.toInt(),
                loseChange=(1 - s).toInt(),
            )
            Db.updatePlayer(guest.id,
                eloChange=(hostEloChange * -1),
                winChange=(1 - s).toInt(),
                loseChange=s.toInt()
            )
        }

        val winner: User? = if (votes[0] == Outcomes.hostWin) { host } else if (votes[0] == Outcomes.guestWin) { guest } else { null }

        tc.sendMessage(EmbedBuilder()
            .setTitle("Game has finished")
                // sets the thumbnail to the winner's pfp, uses host's if draw
            .setThumbnail(try {
                winner!!.avatarUrl
            } catch (e: NullPointerException) {
                host.avatarUrl
            })
            .setColor(Color.green)
            .addField("Changes to elo",
                "${host.name}: $hostEloChange\n" +
                "${guest.name}: ${hostEloChange * -1}",
                false)
            .setFooter(Strings.genericEmbedFooter.content)
            .build())
            .queue()
    }
}