package xyz.colmmurphy.mbac

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.github.cdimascio.dotenv.dotenv
import org.bson.Document
import java.time.Instant

object Db {
    val client =
        MongoClients.create("mongodb+srv://Colm:${dotenv()["DB_PASS"]}@cluster0.n2kg9.mongodb.net/MBAC?retryWrites=true&w=majority")
    val database = client.getDatabase("MBAC")
    val players = database.getCollection("players")

    /**
     * adds a new 'player' to the database, and returns the Document
     */
    fun addNewPlayer(identifier: String): Document {
        players.insertOne(
            Document("id", identifier)
                .append("elo", 1500)
                .append("activeSince", Instant.now().epochSecond * 1000)
                .append("win", 0)
                .append("lose", 0)
                .append("draw", 0)
        )
        return players.find(Document("id", identifier)).first()!!
    }

    /**
     * fetches the Document  associated with the provided id and returns a Player object with all the relevant data
     */
    fun getPlayerFromId(id: String): Player {
        val res = if (players.find(Document("id", id)).first() == null) {
                addNewPlayer(id)
            } else players.find(Document("id", id)).first()

            val playerJson = res.toJson()
        return (Player(id,
            playerJson.substringAfter("elo\": ").substringBefore(",").toInt(),
            playerJson.substringAfter("win\": ").substringBefore(",").toInt(),
            playerJson.substringAfter("lose\": ").substringBefore(",").toInt(),
            playerJson.substringAfter("draw\": ").substringBefore("}").toInt(),
            playerJson.substringAfter("activeSince\": ").substringBefore(",")))
    }

    /**
     * changes the values in a given person's Document
     */
    fun updatePlayer(id: String, eloChange: Int = 0, winChange: Int = 0, loseChange: Int = 0, drawChange: Int = 0) {
        val p = getPlayerFromId(id)
        players.updateOne(eq("id", id),
            combine(
                set("elo", p.elo + eloChange),
                set("win", p.win + winChange),
                set("lose", p.lose + loseChange),
                set("draw", p.draw + drawChange)))
    }
}