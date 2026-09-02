package com.smartspend.ai.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class], version = 8, exportSchema = false)
abstract class SmartSpendDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    companion object {
        @Volatile private var instance: SmartSpendDatabase? = null
        fun get(context: Context): SmartSpendDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, SmartSpendDatabase::class.java, "smartspend.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE transactions ADD COLUMN rawBody TEXT") }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE transactions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amountPaise INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, merchant TEXT, occurredAt INTEGER NOT NULL, source TEXT NOT NULL, sender TEXT, fingerprint TEXT NOT NULL)")
                db.execSQL("INSERT INTO transactions_new (id, amountPaise, type, category, merchant, occurredAt, source, sender, fingerprint) SELECT id, amountPaise, type, category, merchant, occurredAt, source, sender, fingerprint FROM transactions")
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_fingerprint ON transactions (fingerprint)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isCreditCard INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN refId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN rawBody TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT")
            }
        }
private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN customCategory TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN splitInfo TEXT")
            }
        }
    }
}
