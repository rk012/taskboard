package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus

class Goal(name: String): TaskObject(name) {
    override fun updateSelf() {
        if (dependencies.all { it.status == TaskStatus.NOT_STARTED }) {
            status = TaskStatus.NOT_STARTED
            return
        }

        dependencies.forEach {
            if (it.status == TaskStatus.IN_PROGRESS) {
                status = TaskStatus.IN_PROGRESS
                return
            }
        }

        status = TaskStatus.COMPLETE
    }
}