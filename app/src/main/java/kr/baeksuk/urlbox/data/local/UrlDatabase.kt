package kr.baeksuk.urlbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

@Database(entities = [UrlEntity::class], version = 1)
abstract class UrlDatabase : RoomDatabase() {

    abstract fun urlDao(): UrlDao

    companion object {
        @Volatile
        private var INSTANCE: UrlDatabase? = null

        fun getInstance(context: Context): UrlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UrlDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `keyword_history_new` " +
                            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`keyword` TEXT NOT NULL, " +
                            "`result` TEXT NOT NULL, " +
                            "`quiz` TEXT NOT NULL, " +
                            "`localDate` TEXT NOT NULL)"
                )
                database.execSQL(
                    "INSERT INTO keyword_history_new (id, keyword, result, quiz) " +
                            "SELECT id, keyword, result, quiz FROM keyword_history"
                )
                database.execSQL("DROP TABLE keyword_history")
                database.execSQL("ALTER TABLE keyword_history_new RENAME TO keyword_history")
            }
        }
    }

}