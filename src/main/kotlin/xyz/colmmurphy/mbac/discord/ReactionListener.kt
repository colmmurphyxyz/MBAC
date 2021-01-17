package xyz.colmmurphy.mbac.discord

import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class ReactionListener: ListenerAdapter() {
    val a = 123
    fun main() {
        println("hello world")
    }
    override fun onMessageReactionAdd(e: MessageReactionAddEvent) {
        //if (e.reaction.reactionEmote.isEmoji) println(e.reaction.reactionEmote.asCodepoints)
    }
}