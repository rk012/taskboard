import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
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

    private val path: Path? by option(
        "-p", "--path",
        help = "Path where the new taskboard will be saved"
    ).convert { Path(it) }

    private val override: Boolean by option(
        "-o", "-override",
        help = "If set, deletes specified file if exists when creating a new taskboard"
    ).flag()

    override fun run() {
        val path = path ?: Path("$name.json")

        if (path.exists() && !override) {
            echo("File specified already exists. Run command with -o option to overwrite.")
            return
        }

        path.deleteIfExists()
        path.createFile()
        path.writeText(Taskboard(name).toJson())

        val configPath = Path(Context.FNAME)
        configPath.writeText(path.toRealPath().toString())
    }
}

fun main(args: Array<String>) = BaseCommand().subcommands(
    Init()
).main(args)