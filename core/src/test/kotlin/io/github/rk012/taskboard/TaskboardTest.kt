package io.github.rk012.taskboard

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

class TaskboardTest {
    private lateinit var tb: Taskboard

    @BeforeEach
    fun createTaskboard() { tb = Taskboard("Test Taskboard") }

    @Test
    fun removeTest() {
        val t = tb.createTask("Test Task")

        assertTrue(tb.removeObject(t))
        assertFalse(tb.removeObject(t))
    }

    @Test
    fun dependencySortTest() {
        val t0 = tb.createTask("Task 0")
        val t1 = tb.createTask("Task 1")
        val t2 = tb.createTask("Task 2")

        t0.addDependency(t1)
        t1.addDependency(t2)

        assertEquals(
            listOf(t2, t1, t0),
            tb.sortByDependents()
        )
    }

    @Test
    fun idTest() {
        val t0 = tb.createTask("Test Task")
        assertEquals(8, t0.id.length)
        assertEquals(t0, tb[t0.id])
    }
}