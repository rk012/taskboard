package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus

class Goal internal constructor(name: String, id: String): TaskObject(name, id) {
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
}