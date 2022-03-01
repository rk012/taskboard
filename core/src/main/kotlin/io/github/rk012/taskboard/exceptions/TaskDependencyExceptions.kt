package io.github.rk012.taskboard.exceptions

sealed class TaskDependencyBaseException : TaskboardBaseException()

class NoSuchDependencyException : TaskDependencyBaseException()
class CircularDependencyException : TaskDependencyBaseException()
class DependencyAlreadyExistsException : TaskDependencyBaseException()
