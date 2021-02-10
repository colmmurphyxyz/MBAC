package xyz.colmmurphy.mbac


import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.colmmurphy.mbac.discord.Bot


fun main() {
    // Disable MongoDB logging in general
    System.setProperty("DEBUG.MONGO", "false")

    // Disable DB operation tracing
    System.setProperty("DB.TRACE", "false")

    Bot.onEnable()
}

