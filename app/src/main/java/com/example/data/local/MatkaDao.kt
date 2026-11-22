package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MatkaDao {

    @Query("SELECT * FROM markets ORDER BY isFavorite DESC, name ASC")
    fun observeAllMarkets(): Flow<List<MarketEntity>>

    @Query("SELECT * FROM result_records ORDER BY dateIso DESC, createdAt DESC")
    fun observeAllRecords(): Flow<List<ResultRecordEntity>>

    @Query("SELECT * FROM result_records WHERE marketName = :marketName ORDER BY dateIso DESC, createdAt DESC")
    fun observeRecordsForMarket(marketName: String): Flow<List<ResultRecordEntity>>

    @Query("SELECT * FROM markets WHERE name = :name LIMIT 1")
    suspend fun getMarketByName(name: String): MarketEntity?

    @Query("SELECT * FROM markets ORDER BY isFavorite DESC, name ASC")
    suspend fun getAllMarketsOnce(): List<MarketEntity>

    @Query("SELECT * FROM result_records ORDER BY dateIso DESC")
    suspend fun getAllRecordsOnce(): List<ResultRecordEntity>

    @Query("SELECT * FROM result_records WHERE marketName = :marketName ORDER BY dateIso DESC")
    suspend fun getRecordsForMarketOnce(marketName: String): List<ResultRecordEntity>

    @Query("SELECT * FROM result_records WHERE marketName = :marketName AND dateIso = :dateIso LIMIT 1")
    suspend fun getRecordByMarketAndDate(marketName: String, dateIso: String): ResultRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMarketIgnore(market: MarketEntity): Long

    @Query("UPDATE markets SET lastSyncedAt = :timestamp WHERE name = :name")
    suspend fun updateMarketSyncTimestamp(name: String, timestamp: Long)

    @Query("UPDATE markets SET isFavorite = :isFavorite WHERE name = :name")
    suspend fun setMarketFavorite(name: String, isFavorite: Boolean)

    @Query(
        "UPDATE markets SET openTimeMinutes = :openMinutes, closeTimeMinutes = :closeMinutes, offDaysCsv = :offDaysCsv WHERE name = :name"
    )
    suspend fun updateMarketSchedule(
        name: String,
        openMinutes: Int,
        closeMinutes: Int,
        offDaysCsv: String
    )

    @Query("DELETE FROM markets WHERE name = :marketName")
    suspend fun deleteMarket(marketName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: ResultRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecords(records: List<ResultRecordEntity>)

    @Query("UPDATE result_records SET isVerified = :verified WHERE id = :id")
    suspend fun setRecordVerified(id: Long, verified: Boolean)

    @Query("DELETE FROM result_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM result_records WHERE marketName = :marketName")
    suspend fun deleteRecordsForMarket(marketName: String)

    @Query("DELETE FROM result_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM markets")
    suspend fun deleteAllMarkets()

    @Transaction
    suspend fun deleteMarketWithRecords(marketName: String) {
        deleteRecordsForMarket(marketName)
        deleteMarket(marketName)
    }

    @Transaction
    suspend fun replaceAllData(markets: List<MarketEntity>, records: List<ResultRecordEntity>) {
        deleteAllRecords()
        deleteAllMarkets()
        markets.forEach { insertMarketIgnore(it) }
        upsertRecords(records)
    }
}
