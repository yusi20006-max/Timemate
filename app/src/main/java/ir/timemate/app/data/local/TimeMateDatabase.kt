package ir.timemate.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.timemate.app.data.model.AlarmEntity
import ir.timemate.app.data.model.AlarmHistoryEntity
import ir.timemate.app.data.model.Converters
import ir.timemate.app.data.model.HabitEntity
import ir.timemate.app.data.model.ReminderEntity

@Database(
    entities = [AlarmEntity::class, ReminderEntity::class, HabitEntity::class, AlarmHistoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeMateDatabase : RoomDatabase() {
    abstract fun dao(): TimeMateDao
    abstract fun alarmHistoryDao(): AlarmHistoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS alarm_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alarmId INTEGER NOT NULL,
                        sessionId TEXT NOT NULL,
                        scheduledAtMillis INTEGER NOT NULL,
                        startedAtMillis INTEGER NOT NULL,
                        endedAtMillis INTEGER,
                        snoozeCount INTEGER NOT NULL,
                        durationMillis INTEGER,
                        status TEXT NOT NULL,
                        lastSnoozedAtMillis INTEGER
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_alarm_history_sessionId ON alarm_history(sessionId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_alarm_history_startedAtMillis ON alarm_history(startedAtMillis)"
                )
            }
        }

        @Volatile
        private var INSTANCE: TimeMateDatabase? = null

        fun getInstance(context: Context): TimeMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeMateDatabase::class.java,
                    "timemate_offline.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
