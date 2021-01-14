package xyz.colmmurphy.mbac

import org.litote.kmongo.*

fun main() {
    println("Hello World")
    val pioneer = Player("pioneer", 1500)
    val database = KMongo.createClient().getDatabase("players")
    database.getCollection<Player>().insertOne(pioneer)
}