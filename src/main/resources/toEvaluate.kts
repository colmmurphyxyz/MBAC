import java.io.PrintStream
import java.io.File
import xyz.colmmurphy.mbac.*
import org.bson.Document
import org.bson.conversions.Bson
import com.mongodb.client.MongoCursor
import com.mongodb.BasicDBObject


val ps = PrintStream("src/main/resources/output.txt")
ps.println("Start")


import io.github.cdimascio.dotenv.dotenv
ps.println(dotenv()["API_KEY"])

