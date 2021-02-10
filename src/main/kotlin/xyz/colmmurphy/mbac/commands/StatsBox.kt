package xyz.colmmurphy.mbac.commands

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.bson.Document
import xyz.colmmurphy.mbac.Db
import xyz.colmmurphy.mbac.Player
import xyz.colmmurphy.mbac.enums.Strings
import java.awt.Color

class StatsBox (private val e: GuildMessageReceivedEvent, private val u: User) {
    fun getStats(): MessageEmbed {

        val p: Player = Db.getPlayerFromId(u.id)
        var embedField = ""

        embedField += "**id:** ${p.identifier}\n" +
                "**Elo:** ${p.elo}\n" +
                "**Games played:** ${p.win + p.lose + p.draw}\n" +
                "**Wins:** ${p.win}\n" +
                "**Losses:** ${p.lose}\n" +
                "**Draws:** ${p.draw}\n" +
                "**Win/Loss ratio:** ${try {
                    p.win.toFloat().div(p.lose).toString().substring(0, 4)
                } catch (err: StringIndexOutOfBoundsException) {
                    p.win.toFloat().div(p.lose).toString()
                } catch (err: ArithmeticException) {
                    "N/A"
                }
                }\n" +
                "**Active Since:** ${java.time.format.DateTimeFormatter.ISO_INSTANT
                    .format(java.time.Instant.ofEpochSecond(p.activeSince.toLong()/1000))
                    .substring(0, 10)}"

        val msg = EmbedBuilder().setTitle("${u.name}#${u.discriminator}'s stats")
            .setThumbnail(u.avatarUrl)
            .setColor(Color.blue)
            .addField(" ",
            embedField,
            true)
            .setFooter(Strings.genericEmbedFooter.content)

        return msg.build()
    }
}