package io.github.rk012.taskboard.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class SerializableTaskObject(
    val id: String,
    val name: String,
    val time: String,

    val labels: List<String>,
    val isComplete: Boolean,

    val dependencies: List<String>
)