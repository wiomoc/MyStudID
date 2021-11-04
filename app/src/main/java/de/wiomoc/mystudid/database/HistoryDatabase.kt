package de.wiomoc.mystudid.database

import android.content.Context
import androidx.paging.DataSource
import androidx.room.*
import java.util.*

@Database(
    entities = [HistoryDatabase.Transaction::class, HistoryDatabase.TransactionPosition::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(HistoryDatabase.Converters::class)
abstract class HistoryDatabase : RoomDatabase() {
    @Entity(primaryKeys = ["cardID", "onlineID"], indices = [Index("date"), Index("onlineID", unique = true)])
    data class Transaction(
        val cardID: Int = 0, val onlineID: String = "", val date: Date,
        val amount: Float?, val location: String? = null, var credit: Float? = null
    )

    @Entity(primaryKeys = ["onlineID", "positionID"], indices = [Index("onlineID")],
            foreignKeys = [ForeignKey(entity = Transaction::class, parentColumns = ["onlineID"], childColumns = ["onlineID"], onDelete = ForeignKey.CASCADE)]
    )
    data class TransactionPosition(val onlineID: String, val positionID: Int,
                                   val amount: Float, val title: String)

    @Dao
    interface HistoryDao {
        @Query("Select * FROM 'transaction' ORDER BY `date` DESC")
        fun getAllTransactions(): DataSource.Factory<Int, Transaction>

        @Query("Select * FROM 'transaction' WHERE `credit` IS NULL ORDER BY `date`")
        fun getAllTransactionsWithoutCredit(): List<Transaction>

        @Query("Select * FROM 'transaction' WHERE `date` > :date AND `cardID` != 0")
        fun getAllCardTransactionsAfter(date: Date): List<Transaction>

        @Query("Select * FROM 'transaction' WHERE `date` < :date ORDER BY `date` DESC LIMIT 1")
        fun getTransactionBefore(date: Date): Transaction?

        @Query("Select Max(date) FROM 'transaction' WHERE `onlineID` != \"\"")
        fun getLastOnlineTransactionDate(): Date?

        @Query("Select * FROM 'transaction' ORDER BY `date` DESC LIMIT 1")
        fun getLastTransaction(): Transaction?

        @Query("Select count(1) where exists (select * from 'transaction')")
        fun hasTransactions(): Int?

        @Update
        fun updateTransactions(transactions: List<Transaction>)

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insertOnlineTransactions(transactions: List<Transaction>)

        @Delete
        fun deleteTransaction(transaction: Transaction)

        @Query("DELETE FROM 'transaction'")
        fun deleteAllTransactions()
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
