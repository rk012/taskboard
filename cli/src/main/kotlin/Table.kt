import com.github.ajalt.clikt.output.TermUi.echo
import io.github.rk012.taskboard.items.TaskObject
import kotlinx.datetime.LocalDateTime
import kotlin.math.max

private val columns = listOf("ID", "Name", "Status", "Time", "Dependents", "Dependencies")

// Extracting stuff from ISO Formatted output
private fun LocalDateTime.formatted() = "${toString().subSequence(0..9)} ${toString().subSequence(11..15)}"

private fun getPad(items: List<TaskObject>): List<Int> = listOf(
    max(columns[0].length, items.maxOf { it.id.length }),
    max(columns[1].length, items.maxOf { it.name.length }),
    max(columns[2].length, items.maxOf { it.status.name.length }),
    max(columns[3].length, items.maxOf { it.time.formatted().length }),
    max(columns[4].length, items.maxOf { it.getDependents().size.toString().length }),
    max(columns[5].length, items.maxOf { it.getDependencies().size.toString().length }),
)

fun printTable(items: List<TaskObject>, extraPad: Int = 2) {
    val pad = getPad(items)

    echo(columns.mapIndexed { i, s -> s.padEnd(pad[i] + extraPad) }.joinToString("|"))
    echo(pad.joinToString("|") { "-".repeat(it + extraPad) })

    items.forEach {
        echo(
            listOf(
                it.id,
                it.name,
                it.status.name,
                it.time.formatted(),
                it.getDependents().size.toString(),
                it.getDependencies().size.toString()
            ).mapIndexed { i, s -> s.padEnd(pad[i] + extraPad) }.joinToString("|")
        )
    }
}