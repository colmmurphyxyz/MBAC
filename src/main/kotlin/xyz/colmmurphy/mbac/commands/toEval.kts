import java.io.PrintStream
import org.bson.Document
import org.bson.conversions.Bson
import xyz.colmmurphy.mbac.Db
import xyz.colmmurphy.mbac.Player
import xyz.colmmurphy.mbac.enums.values
import xyz.colmmurphy.mbac.commands.Commands
import xyz.colmmurphy.mbac.commands.Commands.*
import java.util.concurrent.TimeUnit
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCursor
import xyz.colmmurphy.mbac.enums.Strings

val old = System.out
System.setOut(PrintStream("toEval.output.txt"))


Db.updatePlayer("417097416085602315", eloChange=-500)


System.out.flush()
System.setOut(old)

