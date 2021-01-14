package xyz.colmmurphy.mbac

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.*

class Player(val name: String, val elo: Int) {
    val client = KMongo.createClient()
    val database: MongoDatabase = client.getDatabase("players")
    val col = database.getCollection<Player>()
}
