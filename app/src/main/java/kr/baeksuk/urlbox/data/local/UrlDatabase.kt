package kr.baeksuk.urlbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

@Database(entities = [UrlEntity::class, UrlBackupEntity::class], version = 4) // 버전 증가
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // 마이그레이션 추가
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 기존 테이블의 모든 데이터를 유지하면서 새 테이블 생성
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS url_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        urlLink TEXT NOT NULL,
                        imageKey TEXT NOT NULL,
                        favorite INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // 기존 테이블 데이터 복사
                database.execSQL("""
                    INSERT INTO url_history_new (id, urlLink, imageKey, favorite)
                    SELECT id, urlLink, imageKey, favorite FROM url_history
                """)

                // 기존 테이블 삭제
                database.execSQL("DROP TABLE url_history")

                // 새로운 테이블을 기존 테이블 이름으로 변경
                database.execSQL("ALTER TABLE url_history_new RENAME TO url_history")
            }
        }

        val MIGRATION_2_3 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // UrlBackupEntity 테이블 생성
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
    }
}
