package io.github.rk012.taskboard.exceptions

class NoSuchLabelException(val labelName: String) : TaskboardBaseException() {
    override val message: String = "Label \"${labelName}\" does not exist"
}