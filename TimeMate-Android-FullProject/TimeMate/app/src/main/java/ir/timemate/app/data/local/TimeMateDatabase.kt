package ir.timemate.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ir.timemate.app.data.model.AlarmEntity
import ir.timemate.app.data.model.Converters
import ir.timemate.app.data.model.HabitEntity
import ir.timemate.app.data.model.ReminderEntity

@Database(
    entities = [AlarmEntity::class, ReminderEntity::class, HabitEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeMateDatabase : RoomDatabase() {
    abstract fun dao(): TimeMateDao

    companion object {
        @Volatile
        private var INSTANCE: TimeMateDatabase? = null

        fun getInstance(context: Context): TimeMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeMateDatabase::class.java,
                    "timemate_offline.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
