package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.Taskboard
import io.github.rk012.taskboard.serialization.SerializableTaskObject
import kotlinx.datetime.LocalDateTime

class Goal internal constructor(name: String, id: String, time: LocalDateTime): TaskObject(name, id, time) {
    private val dependencyIDs = mutableListOf<String>()

    private constructor(name: String, id: String, time: LocalDateTime, dependencies: List<String>) : this(name, id, time) {
        dependencyIDs.addAll(dependencies)
    }

    companion object {
        internal fun createFromSerializable(s: SerializableTaskObject): Goal {
            val goal = Goal(s.name, s.id, LocalDateTime.parse(s.time), s.dependencies)
            goal.labels.addAll(s.labels)
            return goal
        }
    }

    override fun updateSelf() {
        if (dependencies.all { it.status == TaskStatus.NOT_STARTED }) {
            status = TaskStatus.NOT_STARTED
            return
        }

        dependencies.forEach {
            if (it.status != TaskStatus.COMPLETE) {
                status = TaskStatus.IN_PROGRESS
                return
            }
        }

        status = TaskStatus.COMPLETE
    }

    internal fun toSerializable() = SerializableTaskObject(
        id,
        name,
        time.toString(),
        labels,
        status == TaskStatus.COMPLETE,
        dependencies.map { it.id }
    )

    internal fun loadDependencies(tb: Taskboard) {
        dependencyIDs.forEach {
            addDependency(tb[it]!!)
        }
    }
}