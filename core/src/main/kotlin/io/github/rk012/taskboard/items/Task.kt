package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.exceptions.MissingTaskReqsException

class Task internal constructor(name: String): TaskObject(name) {
    private var isComplete = false

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