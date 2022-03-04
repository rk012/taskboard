import io.github.rk012.taskboard.Taskboard
import java.nio.file.Path
import kotlin.io.path.*

object Context {
    const val FNAME = ".taskboardpath"

    val savePath: Path? = run {
        val path = Path(FNAME)
        if (path.notExists()) {
            path.createFile()
            return@run null
        }

        Path(path.readText())
    }
    val tb = savePath?.let { Taskboard.fromJson(it.readText()) }
}