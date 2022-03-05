import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.*
import io.github.rk012.taskboard.Taskboard
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
    )
        .convert { Path(it) }
        .defaultLazy { Path("$name.json") }
        .check("File specified already exists. Run command with -o option to overwrite.") { overwrite || !path.exists() }

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
    ).convert { Path(it) }.validate {
        require(it.exists()) { "File specified does not exist" }
        require(
            try {
                Taskboard.fromJson(it.readText())
                true
            } catch (e: SerializationException) {
                false
            }
        ) { "File contains invalid JSON data" }
    }

    override fun run() {
        Context.configPath.writeText(path.toRealPath().toString())

        echo("Loaded ${path.toRealPath()}")
    }
}

class Status : CliktCommand(help = "Shows opened taskboard") {
    override fun run() {
        if (Context.tb == null) {
            echo("Nothing is currently opened")
            return
        }

        echo(Context.configPath)
        echo("${Context.tb.name} @ ${Context.savePath!!.toRealPath()}")
    }
}

fun main(args: Array<String>) = BaseCommand().subcommands(
    Init(),
    Open(),
    Status()
).main(args)