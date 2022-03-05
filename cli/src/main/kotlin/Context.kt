import io.github.rk012.taskboard.Taskboard
import java.nio.file.Path
import kotlin.io.path.*

object Context {
    private const val FNAME = ".taskboardpath"
    private val parentDir = this::class.java.protectionDomain.codeSource.location.toURI().toPath().parent.toString()

    val configPath: Path = Path(parentDir, FNAME)

    val savePath: Path? = run {
        if (configPath.notExists()) {
            configPath.createFile()
            return@run null
        }

        val path = configPath.readText()
        if (path.isNotBlank()) Path(configPath.readText()) else null
    }

    val tb = savePath?.let { Taskboard.fromJson(it.readText()) }

    fun saveFile() = savePath?.writeText(tb!!.toJson())
}