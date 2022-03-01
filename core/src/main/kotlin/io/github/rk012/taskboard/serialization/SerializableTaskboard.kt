package io.github.rk012.taskboard.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class SerializableTaskboard(
    val name: String,

    val labels: List<String>,

    val tasks: List<SerializableTaskObject>,
    val goals: List<SerializableTaskObject>
)