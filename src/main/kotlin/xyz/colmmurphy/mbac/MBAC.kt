package xyz.colmmurphy.mbac

//import org.litote.kmongo.*

import com.mongodb.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.*
import javax.security.auth.login.LoginException
import com.mongodb.client.MongoClients

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.github.cdimascio.dotenv.dotenv
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.util.idValue


@Throws(LoginException::class, InterruptedException::class)
fun main() {
    println("Hello World")
    println(dotenv()["DB_PASS"])

}
