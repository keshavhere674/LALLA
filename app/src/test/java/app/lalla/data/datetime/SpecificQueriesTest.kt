package app.lalla.data.datetime

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecificQueriesTest {

    @Test
    fun `verify all 4 user queries produce deterministic 2026 responses`() {
        // Query 1: Aaj kya date hai?
        val q1 = DateTimeHelper.tryResolveDateOrTime("Aaj kya date hai?")
        println("Q1 (Aaj kya date hai?): $q1")
        assertNotNull(q1)
        assertTrue(q1!!.contains("2026"))
        assertTrue(q1.contains("November"))

        // Query 2: Aaj kaunsa din hai?
        val q2 = DateTimeHelper.tryResolveDateOrTime("Aaj kaunsa din hai?")
        println("Q2 (Aaj kaunsa din hai?): $q2")
        assertNotNull(q2)
        assertTrue(q2!!.contains("Friday"))

        // Query 3: Abhi kitne baje hain?
        val q3 = DateTimeHelper.tryResolveDateOrTime("Abhi kitne baje hain?")
        println("Q3 (Abhi kitne baje hain?): $q3")
        assertNotNull(q3)
        assertTrue(q3!!.contains(":")) // Time format e.g. 06:42 PM

        // Query 4: Kal kya date hogi?
        val q4 = DateTimeHelper.tryResolveDateOrTime("Kal kya date hogi?")
        println("Q4 (Kal kya date hogi?): $q4")
        assertNotNull(q4)
        assertTrue(q4!!.contains("2026"))
        assertTrue(q4.contains("21")) // Tomorrow is 21st November
    }
}
