package io.github.rk012.taskboard

import io.github.rk012.taskboard.exceptions.NoSuchLabelException
import io.github.rk012.taskboard.items.Goal
import io.github.rk012.taskboard.items.Task
import io.github.rk012.taskboard.items.TaskObject
import io.github.rk012.taskboard.serialization.SerializableTaskboard
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.reflect.KClass

class Taskboard(var name: String) {
    private val taskObjects = mutableMapOf<String, TaskObject>()
    private val labels = mutableListOf<String>()

    enum class SortOptions(internal val comparable: (TaskObject) -> Comparable<*>) {
        DEPENDENTS({ -1 * it.getDependentSet().size }), // negated for descending order
        TIME({ it.time }),
        NAME({ it.name })
    }

    enum class FilterItems(internal val clazz: KClass<*>) {
        TASK(Task::class),
        GOAL(Goal::class),
        ALL(TaskObject::class)
    }

    companion object {
        private fun createFromSerializable(s: SerializableTaskboard): Taskboard {
            val tb = Taskboard(s.name)

            s.labels.forEach { tb.labels.add(it) }

            s.tasks.forEach { tb.taskObjects[it.id] = Task.createFromSerializable(it) }
            s.goals.forEach { tb.taskObjects[it.id] = Goal.createFromSerializable(it) }

            tb.taskObjects.values.forEach {
                when (it) {
                    is Task -> it.loadDependencies(tb)
                    is Goal -> it.loadDependencies(tb)
                }
            }

            return tb
        }

        internal fun fromJson(json: String) = createFromSerializable(Json.decodeFromString(json))
    }

    private fun <T> Collection<T>.containsAny(other: Collection<T>): Boolean {
        other.forEach {
            if (contains(it)) return true
        }

        return false
    }

    operator fun get(id: String) = taskObjects[id]

    fun createTask(
        name: String,
        time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ): Task {
        var id = UUID.randomUUID().toString().split('-').joinToString("")

        if (!taskObjects.containsKey(id.substring(0..7))) {
            id = id.substring(0..7)
        }

        val task = Task(name, id, time)
        taskObjects[task.id] = task
        return task
    }

    fun createGoal(
        name: String,
        time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ): Goal {
        var id = UUID.randomUUID().toString().split('-').joinToString("")

        if (!taskObjects.containsKey(id.substring(0..7))) {
            id = id.substring(0..7)
        }

        val goal = Goal(name, id, time)
        taskObjects[goal.id] = goal
        return goal
    }

    fun removeObject(obj: TaskObject): Boolean {
        if (taskObjects.remove(obj.id) == null) return false

        obj.delink()
        return true
    }

    fun createLabel(name: String): Boolean = if (!hasLabel(name)) {
        labels.add(name)
        true
    } else false

    fun hasLabel(name: String) = labels.contains(name)

    fun addLabel(obj: TaskObject, name: String, createNew: Boolean = false): Boolean {
        if (createNew) createLabel(name)

        return if (hasLabel(name)) {
            obj.labels.add(name)
            true
        } else false
    }

    fun removeLabel(obj: TaskObject, name: String): Boolean = if (!hasLabel(name)) false else obj.labels.remove(name)

    fun deleteLabel(name: String): Boolean = if (!hasLabel(name)) false else {
        taskObjects.values.filter { it.labels.contains(name) }.forEach { it.labels.remove(name) }
        labels.remove(name)
        true
    }

    fun query(
        sortOptions: List<SortOptions> = emptyList(),
        includeLabels: List<String> = emptyList(),
        excludeLabels: List<String> = emptyList(),
        excludeCompleted: Boolean = false,
        excludeNotStarted: Boolean = false,
        filterItem: FilterItems = FilterItems.ALL
    ): List<TaskObject> {
        val sortComparables = mutableListOf<(TaskObject) -> Comparable<*>?>()

        sortOptions.forEach {
            sortComparables.add(it.comparable)
        }

        SortOptions.values().forEach {
            if (!sortComparables.contains(it.comparable)) sortComparables.add(it.comparable)
        }

        includeLabels.forEach {
            if (!hasLabel(it)) throw NoSuchLabelException(it)
        }

        excludeLabels.forEach {
            if (!hasLabel(it)) throw NoSuchLabelException(it)
        }

        return taskObjects.values.filter {
            filterItem.clazz.isInstance(it) &&
                    (includeLabels.isEmpty() || it.labels.containsAny(includeLabels)) &&
                    (!excludeCompleted || it.status != TaskStatus.COMPLETE) &&
                    (!excludeNotStarted || it.status != TaskStatus.NOT_STARTED) &&
                    !it.labels.containsAny(excludeLabels)
        }.sortedWith(compareBy(*sortComparables.toTypedArray()))
    }

    private fun toSerializable() = SerializableTaskboard(
        name,
        labels,
        taskObjects.values.filterIsInstance<Task>().map { it.toSerializable() },
        taskObjects.values.filterIsInstance<Goal>().map { it.toSerializable() }
    )

    fun toJson() = Json.encodeToString(toSerializable())
}