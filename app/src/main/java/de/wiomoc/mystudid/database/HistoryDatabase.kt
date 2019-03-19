package de.wiomoc.mystudid.database

import android.content.Context
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.room.*
import java.util.*

@Database(entities = [HistoryDatabase.Transaction::class, HistoryDatabase.TransactionPosition::class], version = 1)
@TypeConverters(HistoryDatabase.Converters::class)
abstract class HistoryDatabase : RoomDatabase() {
    @Entity(primaryKeys = ["cardID", "onlineID"], indices = [Index("date"), Index("onlineID", unique = true)])
    data class Transaction(val cardID: Int = 0, val onlineID: String = "",
                           val date: Date, val amount: Float?, val location: String)

    @Entity(primaryKeys = ["onlineID", "positionID"], indices = [Index("onlineID")],
            foreignKeys = [ForeignKey(entity = Transaction::class, parentColumns = ["onlineID"], childColumns = ["onlineID"], onDelete = ForeignKey.CASCADE)]
    )
    data class TransactionPosition(val onlineID: String, val positionID: Int,
                                   val amount: Float, val title: String)

    @Dao
    interface HistoryDao {
        @Query("Select * FROM 'transaction'")
        fun getAllTransactions(): DataSource.Factory<Int, Transaction>

        @Query("Select Max(date) FROM 'transaction'")
        fun getLastOnlineTransactionDate(): Date?

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insertOnlineTransaction(transaction: List<Transaction>)

    }

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }
    }

    abstract fun dao(): HistoryDao

    companion object {
        lateinit var database: HistoryDatabase

        fun init(context: Context) {
            database = Room.databaseBuilder(context, HistoryDatabase::class.java, "appDB").build()
        }

        val dao: HistoryDao
            get() = database.dao()
    }
}