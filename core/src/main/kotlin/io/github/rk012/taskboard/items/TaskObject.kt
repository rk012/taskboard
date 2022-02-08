package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.exceptions.*
import java.util.UUID

sealed class TaskObject(val name: String) {
    val id = UUID.randomUUID().toString()

    protected val dependencies = mutableListOf<TaskObject>() // Other TaskObjects this depends on
    private val dependents = mutableListOf<TaskObject>() // Other TaskObjects that depend on this

    fun addDependency(other: TaskObject) {
        if (dependencies.contains(other)) throw DependencyAlreadyExistsException()
        if (hasDependency(other)) throw CircularDependencyException()

        dependencies.add(other)
        other.dependents.add(this)
        update()
    }

    fun hasDependency(other: TaskObject): Boolean {
        if (dependencies.contains(other)) return true

        dependencies.forEach {
            if (it.hasDependency(other)) return true
        }

        return false
    }

    fun removeDependency(other: TaskObject) {
        if (!dependencies.remove(other)) throw NoSuchDependencyException()

        other.dependents.remove(this)
    }

    protected fun update() {
        updateSelf()

        dependents.forEach {
            it.update()
        }
    }

    var status: TaskStatus = TaskStatus.NOT_STARTED
        protected set

    protected abstract fun updateSelf()
}