package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.exceptions.MissingTaskReqsException

class Task(name: String): TaskObject(name) {
    var isComplete = false
        private set

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
}