package app.lalla

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lalla.data.actions.ActionParser
import app.lalla.data.actions.ActionType
import app.lalla.data.settings.AppSettings
import app.lalla.data.tasks.executors.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class TaskExecutionDeviceTest {

    private lateinit var context: Context
    private lateinit var appSettings: AppSettings

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        appSettings = AppSettings(context)
    }

    // ── 1. Master Control ON / OFF ───────────────────────────────────────────

    @Test
    fun testMasterListeningControlPersistence() {
        // Test turning OFF
        appSettings.setBackgroundListeningEnabled(false)
        assertFalse("Master listening must be false when set to false", appSettings.isBackgroundListeningEnabledSync())

        // Test turning ON
        appSettings.setBackgroundListeningEnabled(true)
        assertTrue("Master listening must be true when set to true", appSettings.isBackgroundListeningEnabledSync())

        // Sensitivity test
        appSettings.setWakeWordSensitivity(1500f)
        assertEquals(1500f, appSettings.getWakeWordSensitivitySync(), 0.01f)

        // Restore to safe default OFF
        appSettings.setBackgroundListeningEnabled(false)
        assertFalse(appSettings.isBackgroundListeningEnabledSync())
    }

    // ── 2. Action Parser: Hindi, Hinglish, English ───────────────────────────

    @Test
    fun testActionParserAlarmFormats() {
        // "set alarm for 6:03 AM"
        val action1 = ActionParser.parse("set alarm for 6:03 AM")
        assertNotNull(action1)
        assertEquals(ActionType.ALARM, action1!!.type)
        assertNotNull(action1.triggerTimeMs)

        val cal = Calendar.getInstance().apply { timeInMillis = action1.triggerTimeMs!! }
        assertEquals(6, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(3, cal.get(Calendar.MINUTE))

        // "subah 7 baje alarm laga do"
        val action2 = ActionParser.parse("subah 7 baje alarm laga do")
        assertNotNull(action2)
        assertEquals(ActionType.ALARM, action2!!.type)
        val cal2 = Calendar.getInstance().apply { timeInMillis = action2.triggerTimeMs!! }
        assertEquals(7, cal2.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal2.get(Calendar.MINUTE))
    }

    @Test
    fun testActionParserWhatsAppFormats() {
        val action = ActionParser.parse("Papa ko WhatsApp bhejo saying main kal subah ghar aa raha hoon")
        assertNotNull(action)
        assertEquals(ActionType.WHATSAPP, action!!.type)
        assertEquals("Papa", action.targetContact)
        assertTrue(action.targetMessage!!.contains("main kal subah ghar aa raha hoon"))
    }

    @Test
    fun testActionParserCallHistory() {
        val action1 = ActionParser.parse("last call kiski thi?")
        assertNotNull(action1)
        assertEquals(ActionType.CALL_HISTORY, action1!!.type)

        val action2 = ActionParser.parse("kiska phone aaya tha")
        assertNotNull(action2)
        assertEquals(ActionType.CALL_HISTORY, action2!!.type)
    }

    @Test
    fun testActionParserFileSearch() {
        val action = ActionParser.parse("Documents folder search karo")
        assertNotNull(action)
        assertEquals(ActionType.FILE_SEARCH, action!!.type)
        assertTrue(action.searchQuery!!.contains("document", ignoreCase = true))
    }

    // ── 3. Alarm Task Executor: 6:03 AM ──────────────────────────────────────

    @Test
    fun testAlarmTaskExecutorScheduling() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 3)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val targetMs = cal.timeInMillis

        val result = AlarmTaskExecutor.execute(context, targetMs, "Test 6:03 AM Alarm", 9999L)
        assertTrue("Alarm execution must succeed", result.isSuccess)
        assertNotNull(result.method)
        assertTrue("Spoken feedback must mention alarm time", result.spokenFeedback.contains("06:03") || result.spokenFeedback.contains("6:03"))
    }

    // ── 4. WhatsApp Task Executor ────────────────────────────────────────────

    @Test
    fun testWhatsAppTaskExecutor() {
        val result = WhatsAppTaskExecutor.execute(context, "Papa", "Test message from LALLA")
        assertTrue("WhatsApp execution must succeed on device with WhatsApp installed", result.isSuccess)
        assertTrue("Spoken feedback must mention user confirmation / send tap",
            result.spokenFeedback.contains("send", ignoreCase = true) ||
            result.spokenFeedback.contains("WhatsApp", ignoreCase = true)
        )
    }

    // ── 5. Call History Task Executor ────────────────────────────────────────

    @Test
    fun testCallHistoryTaskExecutor() {
        val result = CallHistoryTaskExecutor.execute(context, null)
        assertTrue("Call history execution must succeed", result.isSuccess)
        assertTrue("Feedback must contain call history details",
            result.spokenFeedback.contains("call", ignoreCase = true)
        )
    }

    // ── 6. File Search Task Executor ─────────────────────────────────────────

    @Test
    fun testFileSearchTaskExecutor() {
        val result = FileSearchTaskExecutor.execute(context, "Documents")
        assertTrue("File search must execute cleanly without crashing", result.isSuccess)
        assertNotNull(result.spokenFeedback)
    }
}
