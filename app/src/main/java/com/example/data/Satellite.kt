package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.io.InputStreamReader

@Entity(tableName = "satellites")
data class Satellite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val unitSize: String, // e.g., "1U", "2U", "3U", "6U", "12U", "Other"
    val weightKg: Double, // Weight in kilograms
    val launchCountry: String,
    val launchAgency: String,
    val launchDate: String, // YYYY-MM-DD format
    val status: String,    // "Orbiting", "De-orbited", "Decayed", "Launch Failure"
    val description: String,
    val missionObjective: String,
    val imageUrl: String,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
)

@Dao
interface SatelliteDao {
    @Query("SELECT * FROM satellites ORDER BY name ASC")
    fun getAllSatellites(): Flow<List<Satellite>>

    @Query("SELECT * FROM satellites WHERE id = :id")
    suspend fun getSatelliteById(id: Int): Satellite?

    @Query("SELECT * FROM satellites WHERE name LIKE '%' || :query || '%' OR launchCountry LIKE '%' || :query || '%' OR launchAgency LIKE '%' || :query || '%'")
    fun searchSatellites(query: String): Flow<List<Satellite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellite(satellite: Satellite): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellites(satellites: List<Satellite>)

    @Update
    suspend fun updateSatellite(satellite: Satellite)

    @Delete
    suspend fun deleteSatellite(satellite: Satellite)

    @Query("SELECT COUNT(*) FROM satellites")
    suspend fun getCount(): Int

    @Query("SELECT * FROM satellites")
    suspend fun getAllSatellitesList(): List<Satellite>

    @Query("DELETE FROM satellites WHERE isCustom = 0")
    suspend fun deleteAllNonCustom()
}

@Database(entities = [Satellite::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun satelliteDao(): SatelliteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "satellite_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SatelliteRepository(private val satelliteDao: SatelliteDao) {
    val allSatellites: Flow<List<Satellite>> = satelliteDao.getAllSatellites()

    fun searchSatellites(query: String): Flow<List<Satellite>> {
        return satelliteDao.searchSatellites(query)
    }

    suspend fun getSatelliteById(id: Int): Satellite? {
        return satelliteDao.getSatelliteById(id)
    }

    suspend fun insert(satellite: Satellite): Long {
        return satelliteDao.insertSatellite(satellite)
    }

    suspend fun update(satellite: Satellite) {
        satelliteDao.updateSatellite(satellite)
    }

    suspend fun delete(satellite: Satellite) {
        satelliteDao.deleteSatellite(satellite)
    }

    suspend fun ensurePopulated(context: Context) {
        val fileSatellites = loadSatellitesFromAsset(context)
        if (fileSatellites.isEmpty()) return
        
        val existingSats = satelliteDao.getAllSatellitesList()
        val favoriteNames = existingSats.filter { it.isFavorite }.map { it.name }.toSet()
        
        satelliteDao.deleteAllNonCustom()
        
        val parsedSats = fileSatellites.map { sat ->
            if (favoriteNames.contains(sat.name)) {
                sat.copy(isFavorite = true)
            } else {
                sat
            }
        }
        satelliteDao.insertSatellites(parsedSats)
    }

    private fun loadSatellitesFromAsset(context: Context): List<Satellite> {
        val satellites = mutableListOf<Satellite>()
        try {
            context.assets.open("satellite-database.json").use { inputStream ->
                val jsonString = inputStream.reader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val missionName = obj.optString("Mission name", "")
                    val organisation = obj.optString("Organisation", "")
                    val nation = obj.optString("Nation", "")
                    val typeUMass = obj.optString("Type (U/mass)", "")
                    val launchDate = obj.optString("Launch date", "")
                    val status = obj.optString("Status", "")
                    val missionDescription = obj.optString("Mission description", "")

                    val unitSize = parseUnitSize(typeUMass)
                    val weightKg = parseWeight(typeUMass)

                    satellites.add(
                        Satellite(
                            name = missionName,
                            unitSize = unitSize,
                            weightKg = weightKg,
                            launchCountry = nation,
                            launchAgency = organisation,
                            launchDate = launchDate,
                            status = status,
                            description = missionDescription,
                            missionObjective = missionDescription,
                            imageUrl = "",
                            isCustom = false,
                            isFavorite = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return satellites
    }

    private fun parseUnitSize(typeStr: String): String {
        val cleaned = typeStr.uppercase()
        return when {
            cleaned.contains("12U") -> "12U"
            cleaned.contains("6U") -> "6U"
            cleaned.contains("3U") -> "3U"
            cleaned.contains("2U") -> "2U"
            cleaned.contains("1U") -> "1U"
            else -> "Other"
        }
    }

    private fun parseWeight(typeStr: String): Double {
        val cleaned = typeStr.lowercase()
        if (cleaned.contains("kg")) {
            try {
                val regex = """([0-9]+(?:\.[0-9]+)?)""".toRegex()
                val match = regex.find(cleaned)
                if (match != null) {
                    return match.value.toDoubleOrNull() ?: 1.0
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return when {
            cleaned.contains("12u") -> 15.0
            cleaned.contains("6u") -> 8.0
            cleaned.contains("3u") -> 4.0
            cleaned.contains("2u") -> 2.5
            cleaned.contains("1u") -> 1.0
            else -> 1.0
        }
    }
}

val Satellite.missionType: String
    get() {
        val text = (this.missionObjective + " " + this.description + " " + this.name).lowercase()
        return when {
            text.contains("imaging") || text.contains("radar") || text.contains("camera") || text.contains("observation") || text.contains("sar") || text.contains("hyperspectral") || text.contains("spectrum") || text.contains("sarsat") -> "سنجش از دور"
            text.contains("communication") || text.contains("radio") || text.contains("telemetry") || text.contains("beacon") || text.contains("transponder") || text.contains("band") || text.contains("broadcast") || text.contains("iot") -> "ارتباطات و مخابرات"
            text.contains("scientific") || text.contains("research") || text.contains("airglow") || text.contains("ionosphere") || text.contains("seismic") || text.contains("magnetic") || text.contains("astron") || text.contains("star") || text.contains("biological") || text.contains("cell survival") || text.contains("radiation") -> "علمی و تحقیقاتی"
            else -> "تست فناوری"
        }
    }

