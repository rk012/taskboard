package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.Taskboard
import io.github.rk012.taskboard.exceptions.MissingTaskReqsException
import io.github.rk012.taskboard.serialization.SerializableTaskObject
import kotlinx.datetime.LocalDateTime

class Task internal constructor(name: String, id: String, time: LocalDateTime) : TaskObject(name, id, time) {
    private val dependencyIDs = mutableListOf<String>()
    private var isComplete = false

    private constructor(
        name: String,
        id: String,
        time: LocalDateTime,
        dependencies: List<String>,
        is_complete: Boolean
    ) : this(name, id, time) {
        this.isComplete = is_complete
        dependencyIDs.addAll(dependencies)
    }

    companion object {
        internal fun createFromSerializable(s: SerializableTaskObject): Task {
            val task = Task(s.name, s.id, LocalDateTime.parse(s.time), s.dependencies, s.isComplete)
            task.labels.addAll(s.labels)
            return task
        }
    }

    override fun updateSelf() {
        dependencies.forEach {
            if (it.status != TaskStatus.COMPLETE) {
                status = TaskStatus.NOT_STARTED
                isComplete = false
                return
            }
        }

        status = if (isComplete) TaskStatus.COMPLETE else TaskStatus.IN_PROGRESS
    }

    fun markAsComplete() {
        if (status != TaskStatus.IN_PROGRESS) throw MissingTaskReqsException()
        isComplete = true
        update()
    }

    fun markAsIncomplete() {
        isComplete = false
        update()
    }

    internal fun toSerializable() = SerializableTaskObject(
        id,
        name,
        time.toString(),
        labels,
        isComplete,
        dependencies.map { it.id }
    )

    internal fun loadDependencies(tb: Taskboard) {
        dependencyIDs.forEach {
            addDependency(tb[it]!!)
        }
    }
}