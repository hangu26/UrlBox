package kr.baeksuk.urlbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.HiddenFolderSecurityEntity
import kr.baeksuk.urlbox.data.local.entity.PreparationTag
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.util.Converters

@Database(
    entities = [
        UrlEntity::class,
        UrlBackupEntity::class,
        TagBackupEntity::class,
        PreparationTag::class,
        HiddenFolderSecurityEntity::class
    ],
    version = 11
)
@TypeConverters(Converters::class)
abstract class UrlDatabase : RoomDatabase() {

    abstract fun urlDao(): UrlDao

    companion object {
        @Volatile
        private var INSTANCE: UrlDatabase? = null

        private fun hasColumn(database: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            database.query("PRAGMA table_info('${tableName}')").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }

        fun getInstance(context: Context): UrlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UrlDatabase::class.java, "urlbox_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS url_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        urlLink TEXT NOT NULL,
                        imageKey TEXT NOT NULL,
                        favorite INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    INSERT INTO url_history_new (id, urlLink, imageKey, favorite)
                    SELECT id, urlLink, imageKey, favorite FROM url_history
                """)
                database.execSQL("DROP TABLE url_history")
                database.execSQL("ALTER TABLE url_history_new RENAME TO url_history")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS url_backup_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        urlLink TEXT NOT NULL,
                        imageKey TEXT NOT NULL,
                        favorite INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tag_backup_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tag TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS tag_prepare_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tag TEXT NOT NULL,
                timeStamp INTEGER NOT NULL,
                urlList TEXT NOT NULL DEFAULT '[]'
            )
        """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tag_prepare_history RENAME TO tag_prepare_history_old")

                database.execSQL("""
            CREATE TABLE tag_prepare_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tag TEXT NOT NULL,
                timeStamp INTEGER NOT NULL
            )
        """.trimIndent())

                database.execSQL("""
            INSERT INTO tag_prepare_history (id, tag, timeStamp)
            SELECT id, tag, timeStamp FROM tag_prepare_history_old
        """.trimIndent())

                database.execSQL("DROP TABLE tag_prepare_history_old")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hidden_folder_security (
                        userId TEXT NOT NULL PRIMARY KEY,
                        password TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!hasColumn(database, "url_history", "hidden")) {
                    database.execSQL("ALTER TABLE url_history ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasColumn(database, "url_backup_history", "hidden")) {
                    database.execSQL("ALTER TABLE url_backup_history ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!hasColumn(database, "tag_backup_history", "firebaseTagId")) {
                    database.execSQL("ALTER TABLE tag_backup_history ADD COLUMN firebaseTagId TEXT")
                }
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!hasColumn(database, "tag_backup_history", "tagOrder")) {
                    database.execSQL("ALTER TABLE tag_backup_history ADD COLUMN tagOrder TEXT")
                }
            }
        }
    }
}
