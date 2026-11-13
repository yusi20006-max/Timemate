package ir.timemate.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.timemate.app.data.model.AlarmHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmHistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStartedSession(session: AlarmHistoryEntity): Long

    @Query("UPDATE alarm_history SET snoozeCount = MAX(snoozeCount, :count), lastSnoozedAtMillis = :atMillis WHERE sessionId = :sessionId AND endedAtMillis IS NULL")
    suspend fun recordSnooze(sessionId: String, count: Int, atMillis: Long): Int

    @Query("""
        UPDATE alarm_history
        SET endedAtMillis = :endedAtMillis,
            durationMillis = MAX(0, :endedAtMillis - startedAtMillis),
            status = :status
        WHERE sessionId = :sessionId AND endedAtMillis IS NULL
    """)
    suspend fun finishSession(
        sessionId: String,
        endedAtMillis: Long,
        status: String = AlarmHistoryEntity.STATUS_DISMISSED
    ): Int

    @Query("SELECT * FROM alarm_history WHERE startedAtMillis >= :fromMillis AND startedAtMillis < :untilMillis ORDER BY startedAtMillis DESC")
    fun observeHistoryBetween(fromMillis: Long, untilMillis: Long): Flow<List<AlarmHistoryEntity>>

    @Query("SELECT * FROM alarm_history ORDER BY startedAtMillis DESC LIMIT :limit")
    fun observeRecentHistory(limit: Int = 100): Flow<List<AlarmHistoryEntity>>

    @Query("SELECT * FROM alarm_history WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): AlarmHistoryEntity?

    // Only completed records older than the retention window are eligible for cleanup.
    @Query("DELETE FROM alarm_history WHERE endedAtMillis IS NOT NULL AND endedAtMillis < :cutoffMillis")
    suspend fun deleteCompletedBefore(cutoffMillis: Long): Int

    @Query("DELETE FROM alarm_history WHERE id IN (SELECT id FROM alarm_history WHERE endedAtMillis IS NOT NULL ORDER BY endedAtMillis ASC LIMIT :excessCount)")
    suspend fun trimOldestCompleted(excessCount: Int): Int
}
