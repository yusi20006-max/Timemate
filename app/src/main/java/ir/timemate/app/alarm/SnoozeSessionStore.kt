package ir.timemate.app.alarm

import android.content.Context
import java.util.UUID

data class SnoozeSession(
    val alarmId: Int,
    val sessionId: String,
    val snoozeCount: Int,
    val nextTriggerAtMillis: Long,
    val title: String,
    val isVibrate: Boolean,
    val isGradual: Boolean,
    val ringtoneUri: String?
)

object SnoozePreferences {
    private const val PREFS = "timemate_snooze_preferences"
    private const val KEY_MINUTES = "selected_minutes"

    fun getMinutes(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MINUTES, 5)
            .takeIf { it in setOf(5, 10, 15) } ?: 5
    }

    fun setMinutes(context: Context, minutes: Int) {
        if (minutes in setOf(5, 10, 15)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MINUTES, minutes).apply()
        }
    }
}

object SnoozeSessionStore {
    private const val PREFS = "timemate_snooze_sessions"
    private const val KEY_ALARM_IDS = "active_alarm_ids"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(alarmId: Int, field: String) = "alarm_${alarmId}_$field"

    @Synchronized
    fun startNew(
        context: Context,
        alarmId: Int,
        title: String,
        isVibrate: Boolean,
        isGradual: Boolean,
        ringtoneUri: String?
    ): SnoozeSession {
        load(context, alarmId)?.let { previous ->
            AlarmScheduler.cancelSnooze(context, alarmId, previous.sessionId)
        }
        val session = SnoozeSession(
            alarmId = alarmId,
            sessionId = UUID.randomUUID().toString(),
            snoozeCount = 0,
            nextTriggerAtMillis = 0L,
            title = title.removeSuffix(" (چرت)"),
            isVibrate = isVibrate,
            isGradual = isGradual,
            ringtoneUri = ringtoneUri
        )
        save(context, session)
        return session
    }

    @Synchronized
    fun resume(
        context: Context,
        alarmId: Int,
        sessionId: String,
        snoozeCount: Int,
        title: String,
        isVibrate: Boolean,
        isGradual: Boolean,
        ringtoneUri: String?
    ): SnoozeSession {
        val existing = load(context, alarmId)
        val session = if (existing?.sessionId == sessionId) {
            existing.copy(snoozeCount = maxOf(existing.snoozeCount, snoozeCount))
        } else {
            SnoozeSession(
                alarmId, sessionId, snoozeCount, 0L,
                title.removeSuffix(" (چرت)"), isVibrate, isGradual, ringtoneUri
            )
        }
        save(context, session)
        return session
    }

    @Synchronized
    fun savePending(context: Context, session: SnoozeSession) {
        save(context, session)
    }

    @Synchronized
    fun markTriggered(context: Context, session: SnoozeSession) {
        save(context, session.copy(nextTriggerAtMillis = 0L))
    }

    @Synchronized
    fun clear(context: Context, alarmId: Int, sessionId: String) {
        val p = prefs(context)
        if (p.getString(key(alarmId, "session_id"), null) != sessionId) return
        p.edit()
            .remove(key(alarmId, "session_id"))
            .remove(key(alarmId, "snooze_count"))
            .remove(key(alarmId, "next_trigger_at"))
            .remove(key(alarmId, "title"))
            .remove(key(alarmId, "vibrate"))
            .remove(key(alarmId, "gradual"))
            .remove(key(alarmId, "ringtone"))
            .putStringSet(KEY_ALARM_IDS, p.getStringSet(KEY_ALARM_IDS, emptySet()).orEmpty()
                .filterNot { it == alarmId.toString() }.toSet())
            .apply()
    }

    fun getPendingSessions(context: Context): List<SnoozeSession> {
        val p = prefs(context)
        return p.getStringSet(KEY_ALARM_IDS, emptySet()).orEmpty().mapNotNull { rawId ->
            rawId.toIntOrNull()?.let { load(context, it) }
        }.filter { it.nextTriggerAtMillis > 0L }
    }

    private fun load(context: Context, alarmId: Int): SnoozeSession? {
        val p = prefs(context)
        val sessionId = p.getString(key(alarmId, "session_id"), null) ?: return null
        return SnoozeSession(
            alarmId = alarmId,
            sessionId = sessionId,
            snoozeCount = p.getInt(key(alarmId, "snooze_count"), 0),
            nextTriggerAtMillis = p.getLong(key(alarmId, "next_trigger_at"), 0L),
            title = p.getString(key(alarmId, "title"), "بیدارباش") ?: "بیدارباش",
            isVibrate = p.getBoolean(key(alarmId, "vibrate"), true),
            isGradual = p.getBoolean(key(alarmId, "gradual"), true),
            ringtoneUri = p.getString(key(alarmId, "ringtone"), null)
        )
    }

    private fun save(context: Context, session: SnoozeSession) {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_ALARM_IDS, emptySet()).orEmpty().toMutableSet()
        ids.add(session.alarmId.toString())
        p.edit()
            .putString(key(session.alarmId, "session_id"), session.sessionId)
            .putInt(key(session.alarmId, "snooze_count"), session.snoozeCount)
            .putLong(key(session.alarmId, "next_trigger_at"), session.nextTriggerAtMillis)
            .putString(key(session.alarmId, "title"), session.title)
            .putBoolean(key(session.alarmId, "vibrate"), session.isVibrate)
            .putBoolean(key(session.alarmId, "gradual"), session.isGradual)
            .putString(key(session.alarmId, "ringtone"), session.ringtoneUri)
            .putStringSet(KEY_ALARM_IDS, ids)
            .apply()
    }
}
