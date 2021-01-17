package xyz.colmmurphy.mbac.discord

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import xyz.colmmurphy.mbac.Db
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

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

    private val E_h = 1 / (1 + 10.0.pow((guestPlayer.elo - hostPlayer.elo).div(propFactor.toDouble())))
    //the expected outcome of the host, i.e the probability that the host will win
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
                (e.user.equals(host) || e.user.equals(guest)) && bothPlayersReacted(e)
            },
            { e ->
                e.channel.sendMessage("line 73, function returned true").queue()
                waiter.shutdown()
            },

        )

    }

    private var hostWinVotes = 0
    private var guestWinVotes = 0
    private var drawVotes = 0

    private fun bothPlayersReacted(e: GuildMessageReactionAddEvent): Boolean {
        println("called bothPlayersReacted()")
        when(e.reactionEmote.asCodepoints) {
            "U+31U+fe0fU+20e3" -> { //:one:
                hostWinVotes++
                println("increase host votes by 1")
            }
            "U+32U+fe0fU+20e3" -> { //:two:
                guestWinVotes++
                println("increase guest votes by 1")
            }
            "U+33U+fe0fU+20e3" -> { //:three
                drawVotes++
                println("increase draw votes by 1")
            }
        }
        println("$hostWinVotes , $guestWinVotes , $drawVotes")
        if (hostWinVotes == 2) {
            e.channel.sendMessage("${host.name} won, updating database").queue()
            onHostWin()
            return true
        } else if (guestWinVotes == 2) {
            e.channel.sendMessage("${guest.name} won, updating database").queue()
            onGuestWin()
        } else if (drawVotes == 2) {
            e.channel.sendMessage("Tie, updating database").queue()
            onDraw()
        } else return false
        return false
    }
    fun declareWinner(u: User = host, isDraw: String = " ") {
        this.waiter = Bot.waiter
        if (isDraw.toLowerCase() == "draw") {
            onDraw()
        } else if (u.equals(host)) {
            onHostWin()
        } else if (u.equals(guest)) {
            onGuestWin()
        } else throw(NullPointerException())
    }

    fun onHostWin() {
        val hostEloGain = floor(k * (1 - E_h)).toInt()
        val guestEloLoss = ceil(-(k * E_g)).toInt()

        Db.updatePlayer(host.id, eloChange=hostEloGain, winChange=1)
        Db.updatePlayer(guest.id, eloChange=guestEloLoss, loseChange=1)
    }

    fun onGuestWin() {
        val hostEloLoss = ceil(-(k * E_h)).toInt()
        val guestEloGain = floor(k * (1 - E_h)).toInt()

        Db.updatePlayer(host.id, eloChange=hostEloLoss, loseChange=1)
        Db.updatePlayer(guest.id, eloChange = guestEloGain, winChange=1)
    }

    fun onDraw() {
        val hostEloChange = floor(k * (0.5 - E_h)).toInt()
        val guestEloChange = ceil(k * (0.5 - E_g)).toInt()

        Db.updatePlayer(host.id, eloChange=hostEloChange, drawChange=1)
        Db.updatePlayer(guest.id, eloChange=guestEloChange, drawChange=1)
    }
}