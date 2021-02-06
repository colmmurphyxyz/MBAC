package xyz.colmmurphy.mbac.commands

enum class Commands(val aliases: List<String>,
                    val description: String = "Tell Murf to set a description for this command",
                    val ownerOnly: Boolean) {

    EVAL(listOf("eval", "evaluate", "evalcode", "run"),
        "**[Bot owner only]** Runs raw Kotlin code",
        true),

    HELP(listOf("help", "h", "commands"),
        "Displays this menu",
        false),

    STATS(listOf("statsalias", "statistics", "foobar",),
        "Shows your or another person's stats",
        false),

    LEADERBOARD(listOf("lboard", "lb"),
        "Shows the highest-rated players",
        false);

    companion object {

        // returns the enum the provided alias belongs to
        fun belongsTo(s: String): Commands? {
            println("called belongsTo()")
            for (i in Commands.values()) {
                if (i.aliases.contains(s.toLowerCase())) return i
            }
            return null
        }

        //returns a string containing a list of all commands and their descriptions
        fun cmdList(): String {
            var str = ""
            for (i in values()) {
                str += "**${i.name.toLowerCase()}** - ${i.description}\n"
            }
            return str
        }
    }
}