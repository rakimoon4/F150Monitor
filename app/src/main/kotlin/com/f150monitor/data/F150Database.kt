package com.f150monitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OBDReading::class,
        MaintenanceEvent::class,
        Alert::class,
        DiagnosticCode::class,
        TripSummary::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class F150Database : RoomDatabase() {
    
    abstract fun obdReadingDao(): OBDReadingDao
    abstract fun maintenanceEventDao(): MaintenanceEventDao
    abstract fun alertDao(): AlertDao
    abstract fun diagnosticCodeDao(): DiagnosticCodeDao
    abstract fun tripSummaryDao(): TripSummaryDao
    
    companion object {
        @Volatile
        private var INSTANCE: F150Database? = null
        
        fun getDatabase(context: Context): F150Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    F150Database::class.java,
                    "f150_monitor_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
