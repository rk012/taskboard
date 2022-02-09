package io.github.rk012.taskboard.items

import io.github.rk012.taskboard.TaskStatus
import io.github.rk012.taskboard.exceptions.*
import java.util.UUID

sealed class TaskObject(val name: String) {
    val id = UUID.randomUUID().toString()
    var status: TaskStatus = TaskStatus.NOT_STARTED
        protected set

    protected val dependencies = mutableListOf<TaskObject>() // Other TaskObjects this depends on
    private val dependents = mutableListOf<TaskObject>() // Other TaskObjects that depend on this

    init { update() }

    fun addDependency(other: TaskObject) {
        if (dependencies.contains(other)) throw DependencyAlreadyExistsException()
        if (other.hasDependency(this)) throw CircularDependencyException()

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
        update()
    }

    internal fun delink() {
        dependencies.forEach {
            removeDependency(it)
        }

        // List needs to be copied since the removeDependency function modifies the original list
        dependents.toList().forEach {
            it.removeDependency(this)
        }

        update()
    }

    protected fun update() {
        updateSelf()

        dependents.forEach {
            it.update()
        }
    }

    protected abstract fun updateSelf()
}