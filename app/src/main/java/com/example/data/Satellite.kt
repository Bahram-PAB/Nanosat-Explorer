package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT COUNT(*) FROM satellites WHERE name = :name")
    suspend fun checkExists(name: String): Int

    @Query("DELETE FROM satellites WHERE isCustom = 0")
    suspend fun clearPredefined()
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

    suspend fun ensurePopulated() {
        val count = satelliteDao.getCount()
        if (count < 35) { // Force update old database to the new PDF database
            satelliteDao.clearPredefined()
            satelliteDao.insertSatellites(getPredefinedSatellites())
        }
    }

    private fun getPredefinedSatellites(): List<Satellite> {
        return listOf(
            Satellite(
                name = "ESTCube-1",
                unitSize = "1U",
                weightKg = 1.04,
                launchCountry = "Estonia",
                launchAgency = "Tartu Observatory",
                launchDate = "2013-05-07",
                status = "Decayed",
                missionObjective = "Estonian first satellite. Testing of components for the Electric Solar Wind Sail (ESTTube).",
                description = "ESTCube-1 was the first Estonian satellite, developed by students from the University of Tartu and Tartu Observatory. It was launched via a Vega rocket. The primary objective was to deploy a 10-meter tether to measure the force of the solar wind on a conductive line, an essential component for electric solar wind sails.",
                imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "Capella-2",
                unitSize = "Other",
                weightKg = 100.0,
                launchCountry = "United States",
                launchAgency = "Capella Space",
                launchDate = "2020-08-31",
                status = "Orbiting",
                missionObjective = "High-precision commercial radar imagery with Synthetic Aperture Radar (SAR).",
                description = "Capella-2 (formerly Sequoia) is a commercial radar imaging satellite operated by Capella Space. Although classified as a microsatellite due to its 100kg weight, it deploys a massive 8 square meters mesh antenna and can provide incredibly detailed 0.5m resolution images in all weather conditions, day or night.",
                imageUrl = "https://images.unsplash.com/photo-1541185933-ef5d8ed016c2?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "PW-Sat2",
                unitSize = "2U",
                weightKg = 2.5,
                launchCountry = "Poland",
                launchAgency = "Warsaw University of Technology",
                launchDate = "2018-12-03",
                status = "Decayed",
                missionObjective = "Testing an experimental deorbit sail to speed up space debris mitigation.",
                description = "PW-Sat2 was Poland's second CubeSat, built by students at the Warsaw University of Technology. It successfully deployed a large 4m² deorbit sail in January 2021, dramatically increasing its aerodynamic drag and proving that simple sail arrays can clean up space junk far quicker than natural atmospheric decay.",
                imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "GomX-4B",
                unitSize = "6U",
                weightKg = 8.0,
                launchCountry = "Denmark",
                launchAgency = "GomSpace / ESA",
                launchDate = "2018-02-02",
                status = "Orbiting",
                missionObjective = "Inter-satellite cross-links, butane gas propulsion maneuvers, and hyperspectral camera imaging.",
                description = "GomX-4B is a highly sophisticated 6U CubeSat demonstration satellite co-funded by the European Space Agency and built by GomSpace. It flew in formation with GomX-4A across thousands of kilometers, transferring telemetry via radio links and testing an innovative gas-propulsion thruster system.",
                imageUrl = "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "LightSail 2",
                unitSize = "3U",
                weightKg = 5.0,
                launchCountry = "United States",
                launchAgency = "The Planetary Society",
                launchDate = "2019-06-25",
                status = "Decayed",
                missionObjective = "Demonstrating orbital altitude modification purely using photon propulsion from solar sails.",
                description = "LightSail 2 was a crowdfunding-supported project by The Planetary Society. After launching, it successfully deployed a 32-square-meter thin Mylar sail. By aligning the sail dynamically with incoming solar photons, the satellite raised its orbital apogee purely using the momentum of light, verifying solar sailing for future deep-space missions.",
                imageUrl = "https://images.unsplash.com/photo-1464802686167-b939a6910659?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "MOVE-II",
                unitSize = "1U",
                weightKg = 1.2,
                launchCountry = "Germany",
                launchAgency = "Technical University of Munich",
                launchDate = "2018-12-03",
                status = "Orbiting",
                missionObjective = "Testing a dynamic active solar deployment assembly and analyzing advanced solar cells.",
                description = "MOVE-II (Munich Orbital Verification Experiment II) is a student-built 1U CubeSat from TUM. Its key mission is to verify high-efficiency quadruple-junction solar cells under deep-space thermal conditions, using active side panels that expand like butterfly wings after orbital separation.",
                imageUrl = "https://images.unsplash.com/photo-1541186877-35304e35f2a8?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "SwissCube-1",
                unitSize = "1U",
                weightKg = 1.00,
                launchCountry = "Switzerland",
                launchAgency = "EPFL / Swiss Space Center",
                launchDate = "2009-09-23",
                status = "Orbiting",
                missionObjective = "Observing the faint airglow phenomenon in the Earth's upper atmosphere.",
                description = "SwissCube-1 is the first Swiss satellite ever launched. It was built by a consortium of Swiss universities led by EPFL. Still operating occasionally after more than a decade in orbit, it uses an internal near-infrared telescope to capture the emissions of oxygen molecules in the upper atmosphere.",
                imageUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "AAUSAT4",
                unitSize = "1U",
                weightKg = 1.00,
                launchCountry = "Denmark",
                launchAgency = "Aalborg University",
                launchDate = "2016-04-25",
                status = "Orbiting",
                missionObjective = "Global monitoring of marine shipping lines via an Automatic Identification System (AIS).",
                description = "AAUSAT4 is an educational CubeSat built by students at Aalborg University. It aims to test and refine an experimental AIS receiver payload capable of capturing VHF beacons broadcast by transponders of cargo vessels on open oceans, helping monitor global commercial routes.",
                imageUrl = "https://images.unsplash.com/photo-1451186859696-371d9477be93?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "Nayif-1",
                unitSize = "1U",
                weightKg = 1.10,
                launchCountry = "United Arab Emirates",
                launchAgency = "MBRSC / Amateurs",
                launchDate = "2017-02-15",
                status = "Orbiting",
                missionObjective = "Amateur HAM radio transponder communication and engineering training.",
                description = "Nayif-1 is the UAE's first nano-satellite, developed by Emirian students in collaboration with the Mohammed Bin Rashid Space Centre. It delivers a FunCube amateur radio transponder payload, enabling reliable communications and encouraging youth interest in aerospace sciences.",
                imageUrl = "https://images.unsplash.com/photo-1517976487492-5750f3195933?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "Astrocast-01",
                unitSize = "3U",
                weightKg = 4.00,
                launchCountry = "Switzerland",
                launchAgency = "Astrocast SA",
                launchDate = "2018-12-03",
                status = "Orbiting",
                missionObjective = "L-band global constellation development for the internet of things (IoT).",
                description = "Astrocast-01 is a foundational test satellite for Switzerland's commercial Astrocast constellation. It uses custom L-band transceivers to receive updates from remote oil rigs, maritime grids, and farming hardware, demonstrating reliable multi-point global telemetry aggregation.",
                imageUrl = "https://images.unsplash.com/photo-1533134486753-c833f0ed4866?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "QuakeSat",
                unitSize = "3U",
                weightKg = 4.50,
                launchCountry = "United States",
                launchAgency = "Stanford University / NASA",
                launchDate = "2003-06-30",
                status = "Decayed",
                missionObjective = "Correlating ELF magnetic field variations with atmospheric seismic disruptions.",
                description = "QuakeSat was a pioneering 3U CubeSat launched in 2003. Equipped with a deployable 2-foot magnetometer boom, it scanned the ionosphere for extremely low frequency radio fluctuations triggered prior to earthquakes, forming an early model for space-based seismic signal observation.",
                imageUrl = "https://images.unsplash.com/photo-1484589065579-248adc015072?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "SALSAT",
                unitSize = "3U",
                weightKg = 4.50,
                launchCountry = "Germany",
                launchAgency = "Technical University of Berlin",
                launchDate = "2020-09-28",
                status = "Orbiting",
                missionObjective = "Realtime spectrum analyzing and UHF/VHF noise diagnostics in orbit.",
                description = "SALSAT (Spectrum Analysis Satellite) is a mission designed by TU Berlin. It features an ultra-wide band receiver payload to map radio frequency spectrum utilization across Earth, assisting regulatory commissions in identifying unlawful broadcasts or high-strength radio noise sources.",
                imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "PicSat",
                unitSize = "3U",
                weightKg = 4.20,
                launchCountry = "France",
                launchAgency = "Paris Observatory",
                launchDate = "2018-01-12",
                status = "Decayed",
                missionObjective = "Observing exoplanetary planetary transit curves of the Beta Pictoris star system.",
                description = "PicSat was a French scientific 3U CubeSat designed to monitor astronomical targets using a fiber-fed single-mode optical star tracker. It concentrated on the transiting curves of planetary debris and comets surrounding the young, nearby main-sequence star Beta Pictoris.",
                imageUrl = "https://images.unsplash.com/photo-1464802686167-b939a6910659?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "Hiber-1",
                unitSize = "6U",
                weightKg = 7.50,
                launchCountry = "Netherlands",
                launchAgency = "Hiber Global",
                launchDate = "2018-11-29",
                status = "Decayed",
                missionObjective = "Commercial narrowband telemetry service for off-grid industrial systems.",
                description = "Hiber-1 is a commercial 6U CubeSat operated by the Dutch space-tech company Hiber. Using advanced orbital UHF channels, it transfers low-bandwidth data from rural tracking tags, weather monitors, and off-grid infrastructures to centralized analytical hubs.",
                imageUrl = "https://images.unsplash.com/photo-1541186877-35304e35f2a8?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "CubeBug-2 (Manolito)",
                unitSize = "2U",
                weightKg = 1.90,
                launchCountry = "Argentina",
                launchAgency = "Satellogic / Ministry",
                launchDate = "2013-11-21",
                status = "Orbiting",
                missionObjective = "Prototyping Software Defined Radio (SDR) models and amateur optical units in orbit.",
                description = "Manolito (CubeBug-2) was a 2U satellite built in Argentina. It was sponsored by the technology company Satellogic and the Ministry of Science. It validated highly modular aerospace models, custom lithium-ion battery blocks, and an amateur 350-meter resolution camera.",
                imageUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "DeMi",
                unitSize = "6U",
                weightKg = 10.00,
                launchCountry = "United States",
                launchAgency = "MIT / DARPA / NASA",
                launchDate = "2020-05-25",
                status = "Decayed",
                missionObjective = "Validating microelectromechanical (MEMS) mirror arrays for astronomical telescopes.",
                description = "The Deformable Mirror Demonstration (DeMi) was a 6U satellite built by MIT. It successful tested astronomical space telescope technologies, specifically an electro-statically adjustable deformable MEMS mirror grid to correct stellar wavefront distortions in orbit.",
                imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "Swayam",
                unitSize = "1U",
                weightKg = 1.00,
                launchCountry = "India",
                launchAgency = "College of Engineering Pune",
                launchDate = "2016-06-22",
                status = "Orbiting",
                missionObjective = "Establishing passive magnetic stabilization and HAM radio store-and-forward channels.",
                description = "Swayam is a successful 1U nano-satellite built entirely by undergraduate engineering students in Pune, India. It incorporates a unique passive magnetic attitude stabilization system using hysteresis rods and delivers point-to-point amateur radio communications.",
                imageUrl = "https://images.unsplash.com/photo-1517976487492-5750f3195933?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "AlSat-1N",
                unitSize = "3U",
                weightKg = 4.00,
                launchCountry = "Algeria",
                launchAgency = "ASAL / UK Space Agency",
                launchDate = "2016-09-26",
                status = "Orbiting",
                missionObjective = "Validating flexible thin-film solar arrays, a camera, and a deorbit drag sail.",
                description = "AlSat-1N (Algerian Educational NanoSatellite) was fully co-developed by Algerian and British space agencies. It provides a unique payload testing platform in low Earth orbit, deploying flexible thin films to evaluate solar radiation shielding and accelerated orbital drag deceleration.",
                imageUrl = "https://images.unsplash.com/photo-1541185933-ef5d8ed016c2?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "CanX-2",
                unitSize = "3U",
                weightKg = 3.50,
                launchCountry = "Canada",
                launchAgency = "UTIAS / CSA",
                launchDate = "2008-04-28",
                status = "Orbiting",
                missionObjective = "Developing micro-propulsion arrays and spectrometer models for small spacecraft.",
                description = "CanX-2 (Canadian Dwarf Satellite 2) is a highly reliable 3U equivalent satellite created by the University of Toronto. Operating for several years, it demonstrated a revolutionary liquid-propulsion engine, multi-spectral airglow cameras, and advanced GPS trackers.",
                imageUrl = "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "IceEye-X1",
                unitSize = "Other",
                weightKg = 70.00,
                launchCountry = "Finland",
                launchAgency = "ICEYE",
                launchDate = "2018-01-12",
                status = "Orbiting",
                missionObjective = "SAR micro-satellite imaging constellation development.",
                description = "IceEye-X1 is the foundational satellite of the Finnish commercial SAR imaging constellation. It represents a massive breakthrough, packing state-of-the-art Synthetic Aperture Radar technologies into a payload weighing under 100 kg, providing global maritime ice tracking.",
                imageUrl = "https://images.unsplash.com/photo-1451186859696-371d9477be93?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "O/OREOS",
                unitSize = "3U",
                weightKg = 5.50,
                launchCountry = "United States",
                launchAgency = "NASA Ames Research Center",
                launchDate = "2010-11-19",
                status = "Decayed",
                missionObjective = "Astrobiological observation of cell survival and organic chemistry in space.",
                description = "O/OREOS (Organism/Organic Exposure to Orbital Salinity) was an intelligent NASA astrobiology CubeSat. It housed two dynamic payloads: one observing the stability of organic dyes under harsh solar UV light, and another monitoring the activity of living bacteria breeds over a six-month window.",
                imageUrl = "https://images.unsplash.com/photo-1464802686167-b939a6910659?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "SwissCube",
                unitSize = "1U",
                weightKg = 1.00,
                launchCountry = "Switzerland",
                launchAgency = "EPFL",
                launchDate = "2009-09-23",
                status = "Orbiting",
                missionObjective = "Developing educational satellite frameworks and testing stellar optical alignment.",
                description = "SwissCube is Switzerland's historical first satellite. Implemented on a 1U chassis with meticulous engineering margins, it has outlived all solar decay estimates and remains a shining example of university-driven pico-satellite craftsmanship.",
                imageUrl = "https://images.unsplash.com/photo-1533134486753-c833f0ed4866?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "CP1 (CalPoly State)",
                unitSize = "1U",
                weightKg = 1.00,
                launchCountry = "United States",
                launchAgency = "California Polytechnic State Univ",
                launchDate = "2006-07-26",
                status = "Launch Failure",
                missionObjective = "One of the historical earliest CubeSats demonstrating standard P-POD deployments.",
                description = "CP1 was the first CubeSat built at Cal Poly State University, whose researchers co-designed the entire CubeSat standard. Unluckily, its launching Dnepr rocket failed shortly after launch, but CP-1's structure served as the foundation of modern CubeSat container integration.",
                imageUrl = "https://images.unsplash.com/photo-1484589065579-248adc015072?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "SUT-1",
                unitSize = "1U",
                weightKg = 1.00,
                launchCountry = "Thailand",
                launchAgency = "Suranaree University of Technology",
                launchDate = "2021-11-09",
                status = "Orbiting",
                missionObjective = "Mapping space radiation and testing high speed data telemetry downlinks.",
                description = "SUT-1 consists of an educational 1U nanosatellite developed by Thai researchers. Carrying a Geiger-Muller tube counter, it tracks orbital cosmic rays and registers environmental magnetosphere details, broadcasting raw data down to ground stations.",
                imageUrl = "https://images.unsplash.com/photo-1541186877-35304e35f2a8?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "Hera-1 (1HOPSat TD)",
                unitSize = "12U",
                weightKg = 22.0,
                launchCountry = "United States",
                launchAgency = "Hera Systems / US Air Force",
                launchDate = "2019-12-11",
                status = "Orbiting",
                missionObjective = "High-precision commercial and military Earth surveillance imaging.",
                description = "Hera-1 is a highly capable 12U CubeSat equipped with advanced optoelectronic surveillance payloads. Designed to explore sub-meter high-definition optical recordings, it allows continuous remote monitoring and rapid video data transfers.",
                imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "ANGELS",
                unitSize = "12U.",
                weightKg = 20.0,
                launchCountry = "France",
                launchAgency = "CNES / Hemeria",
                launchDate = "2019-12-18",
                status = "Orbiting",
                missionObjective = "Validating Argos Neo receiver payloads for marine and wildlife tracking.",
                description = "ANGELS (Argos Neo on a Generic Economical and Light Satellite) is the first educational-turned-industrial French 12u CubeSat. Developed in partnership with CNES, it operates a miniaturized radio instrument to collect environmental data from remote ground transmitters.",
                imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "FossaSat-1",
                unitSize = "PocketQube 1p",
                weightKg = 0.25,
                launchCountry = "Spain",
                launchAgency = "Fossa Systems",
                launchDate = "2019-12-06",
                status = "Decayed",
                missionObjective = "Testing open-source Lora IoT frequencies on extremely low budget pocket satellites.",
                description = "FossaSat-1 is an incredibly tiny picosatellite built on the 5cm PocketQube standard (1P). Serving low-power amateur nodes around the globe, it proved that miniature, sub-kilogram satellites can enable long-distance internet of things links.",
                imageUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80"
            ),
            Satellite(
                name = "ANDESITE Sensor Node 1",
                unitSize = "0.38 kg",
                weightKg = 0.38,
                launchCountry = "United States",
                launchAgency = "Boston University / US Air Force",
                launchDate = "2020-06-13",
                status = "Orbiting",
                missionObjective = "Ad-Hoc Network Demonstration for Extended Satellite-Based Inquiry and Other Team Endeavors (ANDESITE).",
                description = "ANDESITE Sensor Node 1 is an extremely compact picosatellite deployed from the ANDESITE mother satellite. Weighing just 0.38 kg, it measures spatial variations of the auroral current systems using miniature magnetometers.",
                imageUrl = "https://images.unsplash.com/photo-1541185933-ef5d8ed016c2?w=600&auto=format&fit=crop&q=80"
            )
        )
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

