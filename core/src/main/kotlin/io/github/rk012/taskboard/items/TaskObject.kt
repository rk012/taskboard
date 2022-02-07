package io.github.rk012.taskboard.items

import java.util.UUID

sealed class TaskObject(val name: String) {
    val id = UUID.randomUUID().toString()

    protected val dependencies = mutableListOf<TaskObject>() // Other TaskObjects this depends on
    private val dependents = mutableListOf<TaskObject>() // Other TaskObjects that depend on this

    fun addDependency(other: TaskObject): Boolean {
        // TODO check for circular dependencies
        if (dependencies.contains(other)) return false

        dependencies.add(other)
        other.dependents.add(this)
        update()

        return true
    }

    fun removeDependency(other: TaskObject): Boolean {
        if (!dependencies.remove(other)) return false

        other.dependents.remove(this)

        return true
    }

    private fun update() {
        updateSelf()

        dependents.forEach {
            it.update()
        }
    }

    protected abstract fun updateSelf()
    abstract fun getStatus(): Boolean
}