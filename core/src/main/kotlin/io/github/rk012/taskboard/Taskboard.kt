package io.github.rk012.taskboard

import io.github.rk012.taskboard.items.Goal
import io.github.rk012.taskboard.items.Task
import io.github.rk012.taskboard.items.TaskObject

class Taskboard(val name: String) {
    private val taskObjects = mutableMapOf<String, TaskObject>()

    operator fun get(id: String) = taskObjects[id]

    fun createTask(name: String): Task {
        val task = Task(name)
        taskObjects[task.id] = task
        return task
    }

    fun createGoal(name: String): Goal {
        val goal = Goal(name)
        taskObjects[goal.id] = goal
        return goal
    }

    fun removeObject(obj: TaskObject): Boolean {
        if (taskObjects.remove(obj.id) == null) return false

        obj.delink()
        return true
    }
}