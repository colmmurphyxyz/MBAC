package xyz.colmmurphy.mbac.commands

import de.swirtz.ktsrunner.objectloader.KtsObjectLoader
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths

class Eval(private val e: GuildMessageReceivedEvent, private val input: String) {

    fun writeToFile() {
        val old = System.out
        val ps = PrintStream("src/main/kotlin/xyz/colmmurphy/mbac/commands/toEval.kts")
        System.setOut(ps)

        var toEvaluate = ""
        toEvaluate += "import java.io.PrintStream\n" +
                "import org.bson.Document\n" +
                "import org.bson.conversions.Bson\n" +
                "import xyz.colmmurphy.mbac.Db\n" +
                "import xyz.colmmurphy.mbac.Player\n" +
                "import xyz.colmmurphy.mbac.enums.values\n" +
                "import xyz.colmmurphy.mbac.commands.Commands\n" +
                "import xyz.colmmurphy.mbac.commands.Commands.*\n" +
                "import java.util.concurrent.TimeUnit\n" +
                "import com.mongodb.BasicDBObject\n" +
                "import com.mongodb.client.MongoCursor\n" +
                "import xyz.colmmurphy.mbac.enums.Strings\n" +
                "\nval old = System.out\n" +
                "System.setOut(PrintStream(\"toEval.output.txt\"))\n"
        toEvaluate += "\n$input\n"
        toEvaluate += "\nSystem.out.flush()\n" +
                "System.setOut(old)\n"

        ps.println(toEvaluate)
        System.out.flush()
        System.setOut(old)
    }

    fun runCode() {
        val scriptReader = Files.newBufferedReader(Paths.get("src/main/kotlin/xyz/colmmurphy/mbac/commands/toEval.kts"))
        try {
            val loadedObj = KtsObjectLoader().load<String?>(scriptReader)
        } catch (ignored: IllegalArgumentException) {
            println("Caught IAE")
        }
    }

    fun readOutput(): List<String>
        = File("toEval.output.txt").bufferedReader().readLines()
}