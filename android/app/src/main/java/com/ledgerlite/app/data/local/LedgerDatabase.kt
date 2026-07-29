package com.ledgerlite.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ledgerlite.app.data.local.dao.BigItemDao
import com.ledgerlite.app.data.local.dao.CategoryDao
import com.ledgerlite.app.data.local.dao.ExpenseDao
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.entity.ExpenseRecord

@Database(
    entities = [ExpenseRecord::class, Category::class, BigItem::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bigItemDao(): BigItemDao

    companion object {
        const val DB_NAME = "ledger.db"

        fun create(context: Context): LedgerDatabase =
            Room.databaseBuilder(context.applicationContext, LedgerDatabase::class.java, DB_NAME)
                .addCallback(SeedCallback())
                .fallbackToDestructiveMigration()
                .build()

        fun createInMemory(context: Context): LedgerDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, LedgerDatabase::class.java)
                .allowMainThreadQueries()
                .addCallback(SeedCallback())
                .build()
    }
}

/**
 * 建库时同步插入 8 个默认分类。SupportSQLiteDatabase 非线程安全，不另起协程。
 * 用固定 id + INSERT OR IGNORE 保证幂等。颜色用设计文档主色调的几个变体。
 */
class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        val defaults = listOf(
            SeedCategory(1, "餐饮", 0xFFD95F43, 1, "food"),
            SeedCategory(2, "交通", 0xFF3C6E71, 2, "transport"),
            SeedCategory(3, "购物", 0xFFB57BA6, 3, "shopping"),
            SeedCategory(4, "日用", 0xFF8B9D77, 4, "daily"),
            SeedCategory(5, "娱乐", 0xFFE0A458, 5, "entertainment"),
            SeedCategory(6, "医疗", 0xFFC75D5D, 6, "medical"),
            SeedCategory(7, "学习", 0xFF5B7C99, 7, "study"),
            SeedCategory(8, "其他", 0xFF6D7571, 8, "other"),
        )
        defaults.forEach { c ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, icon, color, sortOrder, isDefault, createdAt, updatedAt) " +
                    "VALUES (?, ?, ?, ?, ?, 1, ?, ?)",
                arrayOf(c.id, c.name, c.icon, c.color, c.sortOrder, now, now)
            )
        }
    }

    private data class SeedCategory(val id: Long, val name: String, val color: Long, val sortOrder: Int, val icon: String)
}
