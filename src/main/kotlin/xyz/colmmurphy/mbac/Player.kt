package xyz.colmmurphy.mbac

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.litote.kmongo.*

import java.time.Instant

class Player(val identifier: String, val elo: Int = 1500, val win: Int = 0, val lose: Int = 0, val draw: Int = 0, val activeSince: String = "") {
    fun addToDb() {
        val newPlayer: Document = Document("id", identifier)
            .append("elo", 1500)
            .append("activeSince", Instant.now().epochSecond * 1000)
            .append("win", 0)
            .append("lose", 0)
            .append("draw", 0)
        MongoClients.create("mongodb+srv://Colm:238Kotlin@cluster0.n2kg9.mongodb.net/MBAC?retryWrites=true&w=majority")
            .getDatabase("MBAC")
            .getCollection("players")
            .insertOne(newPlayer)
        println("Added new Player to database")
    }
}
