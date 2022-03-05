import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.*
import io.github.rk012.taskboard.Taskboard
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
    ).convert { Path(it) }.check("File specified does not exist") { it.exists() }

    override fun run() {
        Context.configPath.writeText(path.toRealPath().toString())

        echo("Loaded ${path.toRealPath()}")
    }
}

fun main(args: Array<String>) = BaseCommand().subcommands(
    Init(),
    Open()
).main(args)