package com.f150monitor.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OBDReadingDao {
    @Insert
    suspend fun insert(reading: OBDReading): Long
    
    @Query("SELECT * FROM obd_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int = 100): Flow<List<OBDReading>>
    
    @Query("SELECT * FROM obd_readings WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getReadingsInTimeRange(startTime: Long, endTime: Long): Flow<List<OBDReading>>
    
    @Query("SELECT * FROM obd_readings WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getReadingsForTrip(tripId: String): Flow<List<OBDReading>>
    
    @Query("SELECT AVG(coolantTemp) FROM obd_readings WHERE timestamp >= :startTime")
    suspend fun getAverageCoolantTemp(startTime: Long): Double?
    
    @Query("SELECT AVG(longFuelTrimBank1) FROM obd_readings WHERE timestamp >= :startTime AND longFuelTrimBank1 IS NOT NULL")
    suspend fun getAverageLongFuelTrim(startTime: Long): Double?
    
    @Query("SELECT * FROM obd_readings WHERE coolantTemp > :threshold ORDER BY timestamp DESC LIMIT 10")
    fun getHighCoolantTempReadings(threshold: Double = 220.0): Flow<List<OBDReading>>
    
    @Query("DELETE FROM obd_readings WHERE timestamp < :olderThan")
    suspend fun deleteOldReadings(olderThan: Long)
    
    @Query("SELECT COUNT(*) FROM obd_readings")
    suspend fun getReadingCount(): Int
}

@Dao
interface MaintenanceEventDao {
    @Insert
    suspend fun insert(event: MaintenanceEvent): Long
    
    @Update
    suspend fun update(event: MaintenanceEvent)
    
    @Delete
    suspend fun delete(event: MaintenanceEvent)
    
    @Query("SELECT * FROM maintenance_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<MaintenanceEvent>>
    
    @Query("SELECT * FROM maintenance_events WHERE completed = 0 ORDER BY nextDueDate ASC")
    fun getUpcomingMaintenance(): Flow<List<MaintenanceEvent>>
    
    @Query("SELECT * FROM maintenance_events WHERE eventType = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEventOfType(type: MaintenanceType): MaintenanceEvent?
    
    @Query("SELECT * FROM maintenance_events WHERE eventType = :type AND completed = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastCompletedEventOfType(type: MaintenanceType): MaintenanceEvent?
}

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: Alert): Long
    
    @Update
    suspend fun update(alert: Alert)
    
    @Query("SELECT * FROM alerts WHERE acknowledged = 0 ORDER BY timestamp DESC")
    fun getUnacknowledgedAlerts(): Flow<List<Alert>>
    
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAlerts(limit: Int = 50): Flow<List<Alert>>
    
    @Query("SELECT * FROM alerts WHERE severity = 'CRITICAL' AND acknowledged = 0")
    fun getCriticalAlerts(): Flow<List<Alert>>
    
    @Query("UPDATE alerts SET acknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Long)
    
    @Query("DELETE FROM alerts WHERE timestamp < :olderThan")
    suspend fun deleteOldAlerts(olderThan: Long)
}

@Dao
interface DiagnosticCodeDao {
    @Insert
    suspend fun insert(code: DiagnosticCode): Long
    
    @Update
    suspend fun update(code: DiagnosticCode)
    
    @Query("SELECT * FROM diagnostic_codes WHERE cleared = 0 ORDER BY timestamp DESC")
    fun getActiveCodes(): Flow<List<DiagnosticCode>>
    
    @Query("SELECT * FROM diagnostic_codes ORDER BY timestamp DESC")
    fun getAllCodes(): Flow<List<DiagnosticCode>>
    
    @Query("UPDATE diagnostic_codes SET cleared = 1, clearedTimestamp = :timestamp WHERE code = :code AND cleared = 0")
    suspend fun markCodeCleared(code: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface TripSummaryDao {
    @Insert
    suspend fun insert(trip: TripSummary)
    
    @Update
    suspend fun update(trip: TripSummary)
    
    @Query("SELECT * FROM trip_summary ORDER BY startTime DESC LIMIT :limit")
    fun getRecentTrips(limit: Int = 20): Flow<List<TripSummary>>
    
    @Query("SELECT * FROM trip_summary WHERE tripId = :tripId")
    suspend fun getTripById(tripId: String): TripSummary?
    
    @Query("SELECT * FROM trip_summary WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveTrip(): TripSummary?
    
    @Query("SELECT AVG(avgSpeed) FROM trip_summary WHERE startTime >= :startTime")
    suspend fun getAverageSpeedSince(startTime: Long): Double?
}
