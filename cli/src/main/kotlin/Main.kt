import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import io.github.rk012.taskboard.Taskboard
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import java.nio.file.Path
import kotlin.io.path.*

class BaseCommand : CliktCommand(name = "taskboard") {
    override fun run() = Unit
}

class Init : CliktCommand(help = "Creates a new taskboard") {
    private val name: String by option(
        "-n", "--name",
        help = "Name of the taskboard to be created"
    ).prompt()

    private val overwrite: Boolean by option(
        "-o", "-overwrite",
        help = "If set, deletes specified file if exists when creating a new taskboard"
    ).flag()

    private val path: Path by option(
        "-p", "--path",
        help = "Path where the new taskboard will be saved"
    ).path(
        canBeDir = false,
    ).defaultLazy {
        Path("$name.json")
    }.check("File specified already exists. Run command with -o option to overwrite.") { overwrite || !path.exists() }

    override fun run() {
        path.deleteIfExists()
        path.createFile()
        path.writeText(Taskboard(name).toJson())

        Context.configPath.writeText(path.toRealPath().toString())

        echo("Created new taskboard \"$name\" at ${path.toRealPath()}")
    }
}

class Open : CliktCommand(help = "Opens an existing taskboard") {
    private val path: Path by argument(
        help = "Path to the taskboard file"
    ).path(
        canBeDir = false,
        mustExist = true,
        mustBeWritable = true,
        mustBeReadable = true
    ).check("File contains invalid JSON data") {
        try {
            Taskboard.fromJson(it.readText())
            true
        } catch (e: SerializationException) {
            false
        }
    }

    override fun run() {
        Context.configPath.writeText(path.toRealPath().toString())

        echo("Loaded ${path.toRealPath()}")
    }
}

class Info : CliktCommand(help = "Shows opened taskboard and information about Task/Goal if ID is given") {
    private val id: String? by argument(help = "ID of the Task/Goal").optional()

    override fun run() {
        if (Context.tb == null) {
            echo("Nothing is currently opened")
            return
        }

        echo("${Context.tb.name} @ ${Context.savePath!!.toRealPath()}")

        if (id != null) {
            val obj = Context.tb[id!!]

            if (obj == null) {
                echo("ID $id does not exist", err = true)
                return
            }

            echo(
                """
                Name: ${obj.name}
                ID: ${obj.id}
                Date: ${obj.time.date} ${obj.time.hour}:${obj.time.minute}
                Status: ${obj.status.name}
                Labels: ${obj.getLabels().joinToString()}
                Dependencies: 
                    ${obj.getDependencies().joinToString("\n") { "[${it.id}] ${it.name}" }}
                    
                Dependents:
                    ${obj.getDependents().joinToString("\n") { "[${it.id}] ${it.name}" }}
            """.trimIndent()
            )
        }
    }
}

class Rename : CliktCommand(help = "Renames the taskboard") {
    private val newName: String by argument(help = "New name for the taskboard")

    override fun run() {
        if (Context.tb == null) {
            echo("Nothing is currently opened", err = true)
            return
        }

        val oldName = Context.tb.name
        Context.tb.name = newName
        echo("Renamed $oldName -> $newName")
        Context.saveFile()
    }
}

class Create : CliktCommand(help = "Creates a new Task/Goal") {
    private enum class ObjectType { TASK, GOAL }

    private val objectType: ObjectType by argument(help = "Type of object (task, goal) to be created").enum()
    private val name: String by argument(help = "Name of the object")

    private val date: LocalDateTime by option(
        "-d",
        "--date",
        help = "Date/time, uses current time by default",
        metavar = "yyyy-mm-dd hh:mm"
    ).convert {
        LocalDateTime.parse(it.split(" ").joinToString("T"))
    }.default(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

    override fun run() {
        if (Context.tb == null) {
            echo("Nothing is currently opened", err = true)
            return
        }

        val (newObj, type) = when (objectType) {
            ObjectType.TASK -> Pair(Context.tb.createTask(name, date), "task")
            ObjectType.GOAL -> Pair(Context.tb.createGoal(name, date), "goal")
        }

        Context.saveFile()
        echo("Created new $type \"${newObj.name}\" with id ${newObj.id}")
    }
}

class Delete : CliktCommand(help = "Deletes a Task/Goal") {
    private val id: String by argument(help = "ID of the Task/Goal")

    override fun run() {
        if (Context.tb == null) {
            echo("Nothing is currently opened", err = true)
            return
        }

        val obj = Context.tb[id]

        if (obj == null) {
            echo("ID $id does not exist", err = true)
            return
        }

        Context.tb.removeObject(obj)
        Context.saveFile()
        echo("Deleted \"${obj.name}\"")
    }
}

fun main(args: Array<String>) = BaseCommand().subcommands(
    Init(),
    Open(),
    Info(),
    Rename(),
    Create(),
    Delete(),
).main(args)