package kr.baeksuk.urlbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.PreparationTag
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.util.Converters
import kr.baeksuk.urlbox.util.util.UrlListInTagConverter

@Database(
    entities = [UrlEntity::class, UrlBackupEntity::class, TagBackupEntity::class, PreparationTag::class], // ✅ PreparationTag 추가
    version = 7 // ✅ 버전 증가
)
@TypeConverters(Converters::class)
abstract class UrlDatabase : RoomDatabase() {

    abstract fun urlDao(): UrlDao

    companion object {
        @Volatile
        private var INSTANCE: UrlDatabase? = null

        fun getInstance(context: Context): UrlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UrlDatabase::class.java, "urlbox_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5, MIGRATION_5_6,MIGRATION_6_7) // ✅ 마이그레이션 추가
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

        val MIGRATION_2_3 = object : Migration(3, 4) {
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

        val MIGRATION_4_5 = object : Migration(4, 5) { // ✅ 추가된 마이그레이션
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
                // 기존 테이블 이름 변경
                database.execSQL("ALTER TABLE tag_prepare_history RENAME TO tag_prepare_history_old")

                // 새로운 구조 테이블 생성
                database.execSQL("""
            CREATE TABLE tag_prepare_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tag TEXT NOT NULL,
                timeStamp INTEGER NOT NULL
            )
        """.trimIndent())

                // 기존 데이터 복사
                database.execSQL("""
            INSERT INTO tag_prepare_history (id, tag, timeStamp)
            SELECT id, tag, timeStamp FROM tag_prepare_history_old
        """.trimIndent())

                // 임시 테이블 삭제
                database.execSQL("DROP TABLE tag_prepare_history_old")
            }
        }
    }
}

