import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.Taskboard
import io.github.rk012.taskboard.items.Task
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import java.nio.file.Path
import kotlin.io.path.*

import Context.tb
import Context.configPath
import Context.savePath
import Context.saveFile
import io.github.rk012.taskboard.exceptions.*

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
                    ${
                    obj.getDependencies()
                        .joinToString("\n" + " ".repeat(4 * 5)) { "${if (it.status == TaskStatus.COMPLETE) "*" else " "} [${it.id}] ${it.name}" }
                }
                    
                Dependents:
                    ${obj.getDependents().joinToString("\n" + " ".repeat(4 * 5)) { "  [${it.id}] ${it.name}" }}
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

class Dependency : CliktCommand(help = "Manage dependencies for a Task/Goal") {
    private enum class Action { ADD, REMOVE }

    private val action: Action by argument(name = "<add|remove>").enum()
    private val objID: String by argument(name = "A", help = "ID of Task/Goal dependent")
    private val otherID: String by argument(name = "B", help = "ID of Task/Goal dependency")

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        val obj = tb[objID] ?: throw PrintMessage("ID $objID does not exist", error = true)
        val other = tb[otherID] ?: throw PrintMessage("ID $otherID does not exist", error = true)

        try {
            when (action) {
                Action.ADD -> {
                    obj.addDependency(other)
                    saveFile()
                    echo("Added $otherID to $objID dependencies")
                }
                Action.REMOVE -> {
                    obj.removeDependency(other)
                    saveFile()
                    echo("Removed $otherID from $objID dependencies")
                }
            }
        } catch (e: NoSuchDependencyException) {
            echo("$otherID is not a dependency of $objID")
        } catch (e: DependencyAlreadyExistsException) {
            echo("$otherID is already a dependency of $objID")
        } catch (e: CircularDependencyException) {
            echo("Error: Circular Dependencies")
        }
    }
}

class Label : CliktCommand() {
    override fun run() = Unit
}

class CreateLabel : CliktCommand(name = "create", help = "Creates a new label") {
    private val name: String by argument(help = HELP_LABEL_CREATE)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        echo(
            if (tb.createLabel(name)) "Created label $name"
            else "Label $name already exists"
        )

        saveFile()
    }
}

class AddLabel : CliktCommand(name = "add", help = "Adds a label to a Task/Goal") {
    private val id: String by argument(help = HELP_OBJECT_ID)
    private val name: String by argument(help = HELP_LABEL)

    private val createNew: Boolean by option(
        "-c",
        "--create-new",
        help = "If set, new label is created if it doesn't exist in the taskboard"
    ).flag()

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)
        val obj = tb[id] ?: throw PrintMessage("ID $id does not exist", error = true)

        echo(
            if (tb.addLabel(obj, name, createNew)) "Added label $name to $id"
            else "Label $name does not exist or is already assigned to $id"
        )

        saveFile()
    }
}

class RemoveLabel : CliktCommand(name = "remove", help = "Removes a label to a Task/Goal") {
    private val id: String by argument(help = HELP_OBJECT_ID)
    private val name: String by argument(help = HELP_LABEL)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)
        val obj = tb[id] ?: throw PrintMessage("ID $id does not exist", error = true)

        echo(
            if (tb.removeLabel(obj, name)) "Removed label $name from $id"
            else "Label $name does not exist or is already not assigned to $id"
        )

        saveFile()
    }
}

class DeleteLabel : CliktCommand(name = "delete", help = "Deletes a label from the taskboard") {
    private val name: String by argument(help = HELP_LABEL)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        echo(
            if (tb.deleteLabel(name)) "Deleted label $name"
            else "Label $name does not exist"
        )

        saveFile()
    }
}

class Complete : CliktCommand(help = "Mark a Task as complete") {
    private val id: String by argument(help = HELP_OBJECT_ID)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)
        val obj = tb[id] ?: throw PrintMessage("ID $id does not exist", error = true)

        if (obj !is Task) throw PrintMessage("$id is not a Task", error = true)

        if (obj.status == TaskStatus.COMPLETE) throw PrintMessage("Task $id is already complete")
        try {
            obj.markAsComplete()
        } catch (e: MissingTaskReqsException) {
            throw PrintMessage("Not all dependencies of Task $id are complete", error = true)
        }
        saveFile()
        echo("Marked task $id as complete")
    }
}

class Incomplete : CliktCommand(help = "Mark a Task as incomplete") {
    private val id: String by argument(help = HELP_OBJECT_ID)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)
        val obj = tb[id] ?: throw PrintMessage("ID $id does not exist", error = true)

        if (obj !is Task) throw PrintMessage("$id is not a Task", error = true)

        if (obj.status != TaskStatus.COMPLETE) throw PrintMessage("Task $id is already incomplete")
        obj.markAsIncomplete()
        saveFile()
        echo("Marked task $id as incomplete")
    }
}

class ListCommand : CliktCommand(name = "list", help = "List Task/Goals with various filters") {
    private val sortOptions: String by option(
        "-o",
        "--order-by",
        help = """
            Specifies how to order query results.
            
            d - Dependents
            t - Time
            n - Name
            
            Default: "dtn"
        """.trimIndent()
    ).default("dtn").check { it.length <= 3 }

    private val includeLabels: List<String> by option(
        "--include-labels",
        "-l",
        help = "List of labels to filter by. Use -r to require all labels"
    ).multiple()

    private val requireAllLabels: Boolean by option(
        "--require-all",
        "-r",
        help = "Require query results to contain ALL labels specified with --include-labels option"
    ).flag()

    private val excludeLabels: List<String> by option(
        "--exclude-labels",
        "-e",
        help = "List of labels to exclude"
    ).multiple()

    private val excludeCompleted: Boolean by option(
        "--exclude-completed",
        "-c",
        help = "Prevent completed tasks/goals from being shown."
    ).flag(
        "--include-completed",
        "-C",
        default = false
    )

    private val excludeNotStarted: Boolean by option(
        "--exclude-not-started",
        "-s",
        help = "Prevent task/goals not started from being shown."
    ).flag(
        "--include-not-started",
        "-S",
        default = false
    )

    private val filterItem: Taskboard.FilterItems by option(
        help = "Display All, Tasks, or Goals only"
    ).switch(
        "-a" to Taskboard.FilterItems.ALL,
        "-t" to Taskboard.FilterItems.TASK,
        "-g" to Taskboard.FilterItems.GOAL
    ).default(Taskboard.FilterItems.ALL)

    override fun run() {
        tb ?: throw PrintMessage(TASKBOARD_NOT_OPEN, error = true)

        try {
            printTable(
                tb.query(
                    sortOptions = sortOptions.map {
                        when (it) {
                            'd' -> Taskboard.SortOptions.DEPENDENTS
                            't' -> Taskboard.SortOptions.TIME
                            'n' -> Taskboard.SortOptions.NAME
                            else -> throw PrintMessage("Error: --order-by can only contain letters d, t, and n")
                        }
                    },
                    includeLabels,
                    requireAllLabels,
                    excludeLabels,
                    excludeCompleted,
                    excludeNotStarted,
                    filterItem
                )
            )
        } catch (e: NoSuchLabelException) {
            echo("Error: Label ${e.labelName} does not exist")
        }

        echo()
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
    Dependency(),
    Label().subcommands(
        CreateLabel(),
        AddLabel(),
        RemoveLabel(),
        DeleteLabel(),
    ),
    Complete(),
    Incomplete(),
    ListCommand(),
).main(args)