import Context.tb
import Context.configPath
import Context.savePath
import Context.saveFile

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
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
        help = HELP_TASKBOARD_NAME_CREATE
    ).prompt()

    private val overwrite: Boolean by option(
        "-o", "-overwrite",
        help = HELP_INIT_OVERWRITE
    ).flag()

    private val path: Path by option(
        "-p", "--path",
        help = HELP_TASKBOARD_PATH_NEW
    ).path(
        canBeDir = false,
    ).defaultLazy {
        Path("$name.json")
    }.check(INIT_FILE_EXISTS) { overwrite || !path.exists() }

    override fun run() {
        path.deleteIfExists()
        path.createFile()
        path.writeText(Taskboard(name).toJson())

        configPath.writeText(path.toRealPath().toString())

        echo("Created new taskboard \"$name\" at ${path.toRealPath()}")
    }
}

class Open : CliktCommand(help = "Opens an existing taskboard") {
    private val path: Path by argument(
        help = HELP_TASKBOARD_PATH
    ).path(
        canBeDir = false,
        mustExist = true,
        mustBeWritable = true,
        mustBeReadable = true
    ).check(OPEN_INVALID_JSON) {
        try {
            Taskboard.fromJson(it.readText())
            true
        } catch (e: SerializationException) {
            false
        }
    }

    override fun run() {
        configPath.writeText(path.toRealPath().toString())

        echo("Loaded ${path.toRealPath()}")
    }
}

class Info : CliktCommand(help = "Shows opened taskboard and information about Task/Goal if ID is given") {
    private val id: String? by argument(help = HELP_OBJECT_ID).optional()

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        echo("${tb.name} @ ${savePath!!.toRealPath()}")

        if (id != null) {
            val obj = tb[id!!] ?: throw PrintMessage("ID $id does not exist", error = true)

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
    private val newName: String by argument(help = HELP_TASKBOARD_NAME_NEW)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        val oldName = tb.name
        tb.name = newName
        echo("Renamed $oldName -> $newName")
        saveFile()
    }
}

class Create : CliktCommand(help = "Creates a new Task/Goal") {
    private enum class ObjectType { TASK, GOAL }

    private val objectType: ObjectType by argument(help = HELP_OBJECT_CREATE).enum()
    private val name: String by argument(help = HELP_OBJECT_NAME)

    private val date: LocalDateTime by option(
        "-d",
        "--date",
        help = HELP_OBJECT_DATE,
        metavar = METAVAR_OBJECT_DATE
    ).convert {
        LocalDateTime.parse(it.split(" ").joinToString("T"))
    }.default(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        val (newObj, type) = when (objectType) {
            ObjectType.TASK -> Pair(tb.createTask(name, date), "task")
            ObjectType.GOAL -> Pair(tb.createGoal(name, date), "goal")
        }

        saveFile()
        echo("Created new $type \"${newObj.name}\" with id ${newObj.id}")
    }
}

class Delete : CliktCommand(help = "Deletes a Task/Goal") {
    private val id: String by argument(help = HELP_OBJECT_ID)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        val obj = tb[id] ?: throw PrintMessage("ID $id does not exist", error = true)

        tb.removeObject(obj)
        saveFile()
        echo("Deleted \"${obj.name}\"")
    }
}

class Config : CliktCommand(help = "Update the name/date of a Task/Goal") {
    private val id: String by argument(help = HELP_OBJECT_ID)

    private val newName: String? by option(
        "-n",
        "--new-name",
        help = HELP_OBJECT_NAME_NEW
    )
    private val newDate: LocalDateTime? by option(
        "-d",
        "--new-date",
        help = HELP_OBJECT_DATE_NEW,
        metavar = METAVAR_OBJECT_DATE
    ).convert {
        LocalDateTime.parse(it.split(" ").joinToString("T"))
    }

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        val obj = tb[id] ?: throw PrintMessage("ID $id does not exist", error = true)

        if (newName != null) {
            obj.name = newName!!
        }

        if (newDate != null) {
            obj.time = newDate!!
        }

        if (newName != null || newDate != null) {
            saveFile()
            echo("Updated task ${obj.id}")
        }
    }
}

fun main(args: Array<String>) = BaseCommand().subcommands(
    Init(),
    Open(),
    Info(),
    Rename(),
    Create(),
    Delete(),
    Config(),
).main(args)