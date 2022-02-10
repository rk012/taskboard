package io.github.rk012.taskboard

import io.github.rk012.taskboard.items.Goal
import io.github.rk012.taskboard.items.Task
import io.github.rk012.taskboard.items.TaskObject
import java.util.UUID

class Taskboard(val name: String) {
    private val taskObjects = mutableMapOf<String, TaskObject>()

    operator fun get(id: String) = taskObjects[id]

    fun createTask(name: String): Task {
        var id = UUID.randomUUID().toString().split('-').joinToString("")

        if (!taskObjects.containsKey(id.substring(0..7))) {
            id = id.substring(0..7)
        }

        val task = Task(name, id)
        taskObjects[task.id] = task
        return task
    }

    fun createGoal(name: String): Goal {
        var id = UUID.randomUUID().toString().split('-').joinToString("")

        if (!taskObjects.containsKey(id.substring(0..7))) {
            id = id.substring(0..7)
        }

        val goal = Goal(name, id)
        taskObjects[goal.id] = goal
        return goal
    }

    fun removeObject(obj: TaskObject): Boolean {
        if (taskObjects.remove(obj.id) == null) return false

        obj.delink()
        return true
    }

    fun sortByDependents() = taskObjects.values.sortedByDescending { it.getDependentSet().size }
}