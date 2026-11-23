package ke.co.afyareport.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ke.co.afyareport.data.local.dao.RegisterDefinitionDao
import ke.co.afyareport.data.local.dao.ReportDao
import ke.co.afyareport.data.local.dao.ReportingSessionDao
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.ReportingSessionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity

@Database(
    entities = [
        RegisterDefinitionEntity::class,
        VariableDefinitionEntity::class,
        CodeOptionEntity::class,
        ReportingSessionEntity::class,
        PatientEntryEntity::class,
        VariableValueEntity::class,
        ReportIndicatorEntity::class,
        ReportSettingsEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AfyaReportDatabase : RoomDatabase() {
    abstract fun registerDefinitionDao(): RegisterDefinitionDao
    abstract fun reportingSessionDao(): ReportingSessionDao
    abstract fun reportDao(): ReportDao

    companion object {
        const val DATABASE_NAME = "afyareport.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE variable_definitions ADD COLUMN registerVersion INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_variable_definitions_registerDefinitionId_registerVersion " +
                        "ON variable_definitions(registerDefinitionId, registerVersion)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `report_settings` (`registerDefinitionId` INTEGER NOT NULL, " +
                        "`sexColumn` TEXT NOT NULL, `maleValue` TEXT NOT NULL, `femaleValue` TEXT NOT NULL, " +
                        "`ageColumn` TEXT NOT NULL, PRIMARY KEY(`registerDefinitionId`), " +
                        "FOREIGN KEY(`registerDefinitionId`) REFERENCES `register_definitions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `report_indicators` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`registerDefinitionId` INTEGER NOT NULL, `section` TEXT NOT NULL, `label` TEXT NOT NULL, " +
                        "`countMode` TEXT NOT NULL, `matchAny` INTEGER NOT NULL, `ageBands` TEXT NOT NULL, " +
                        "`conditionsJson` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`registerDefinitionId`) REFERENCES `register_definitions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_report_indicators_registerDefinitionId` " +
                        "ON `report_indicators` (`registerDefinitionId`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AfyaReportDatabase? = null

        fun getInstance(context: Context): AfyaReportDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AfyaReportDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AfyaReportDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
