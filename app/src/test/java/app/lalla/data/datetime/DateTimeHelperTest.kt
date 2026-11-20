package app.lalla.data.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class DateTimeHelperTest {

    @Test
    fun `detects today date queries in Hinglish, English, and Hindi`() {
        assertEquals(DateTimeHelper.QueryType.TODAY_DATE, DateTimeHelper.detectQueryType("Aaj kya date hai?"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DATE, DateTimeHelper.detectQueryType("aaj ki date kya hai"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DATE, DateTimeHelper.detectQueryType("aaj kitni tarikh hai"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DATE, DateTimeHelper.detectQueryType("What is today's date?"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DATE, DateTimeHelper.detectQueryType("today's date"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DATE, DateTimeHelper.detectQueryType("आज क्या तारीख है"))
    }

    @Test
    fun `detects today day queries`() {
        assertEquals(DateTimeHelper.QueryType.TODAY_DAY, DateTimeHelper.detectQueryType("Aaj kaunsa din hai?"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DAY, DateTimeHelper.detectQueryType("aaj konsa din hai"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DAY, DateTimeHelper.detectQueryType("aaj kya din hai"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DAY, DateTimeHelper.detectQueryType("What day is today?"))
        assertEquals(DateTimeHelper.QueryType.TODAY_DAY, DateTimeHelper.detectQueryType("आज कौन सा दिन है"))
    }

    @Test
    fun `detects time queries`() {
        assertEquals(DateTimeHelper.QueryType.CURRENT_TIME, DateTimeHelper.detectQueryType("Abhi kitne baje hain?"))
        assertEquals(DateTimeHelper.QueryType.CURRENT_TIME, DateTimeHelper.detectQueryType("kya time hua hai"))
        assertEquals(DateTimeHelper.QueryType.CURRENT_TIME, DateTimeHelper.detectQueryType("time kya hai"))
        assertEquals(DateTimeHelper.QueryType.CURRENT_TIME, DateTimeHelper.detectQueryType("What time is it?"))
    }

    @Test
    fun `detects tomorrow queries`() {
        assertEquals(DateTimeHelper.QueryType.TOMORROW_DATE, DateTimeHelper.detectQueryType("Kal kya date hogi?"))
        assertEquals(DateTimeHelper.QueryType.TOMORROW_DATE, DateTimeHelper.detectQueryType("kal kaunsi tarikh hogi"))
        assertEquals(DateTimeHelper.QueryType.TOMORROW_DAY, DateTimeHelper.detectQueryType("Kal kaunsa din hoga?"))
        assertEquals(DateTimeHelper.QueryType.TOMORROW_DAY, DateTimeHelper.detectQueryType("kal konsa din hai"))
    }

    @Test
    fun `detects yesterday queries`() {
        assertEquals(DateTimeHelper.QueryType.YESTERDAY_DATE, DateTimeHelper.detectQueryType("Kal kya date thi?"))
        assertEquals(DateTimeHelper.QueryType.YESTERDAY_DAY, DateTimeHelper.detectQueryType("Kal kaunsa din tha?"))
        assertEquals(DateTimeHelper.QueryType.YESTERDAY_DATE, DateTimeHelper.detectQueryType("What was yesterday's date?"))
    }

    @Test
    fun `resolves date accurately with 2026 year`() {
        val res = DateTimeHelper.tryResolveDateOrTime("Aaj kya date hai?")
        assertNotNull(res)
        val currentYear = ZonedDateTime.now().year.toString()
        assertTrue("Response must contain current year $currentYear", res!!.contains(currentYear))
    }
}
