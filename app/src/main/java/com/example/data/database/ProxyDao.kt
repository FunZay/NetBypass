package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxy_servers ORDER BY latencyMs ASC, lastChecked DESC")
    fun getAllProxies(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxy_servers WHERE isActive = 1 ORDER BY latencyMs ASC")
    fun getActiveProxies(): Flow<List<ProxyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxies(proxies: List<ProxyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxy(proxy: ProxyEntity)

    @Update
    suspend fun updateProxy(proxy: ProxyEntity)

    @Delete
    suspend fun deleteProxy(proxy: ProxyEntity)

    @Query("DELETE FROM proxy_servers WHERE id = :id")
    suspend fun deleteProxyById(id: String)

    @Query("DELETE FROM proxy_servers WHERE isActive = 0")
    suspend fun deleteInactiveProxies()

    @Query("DELETE FROM proxy_servers")
    suspend fun clearAllProxies()

    @Query("SELECT COUNT(*) FROM proxy_servers")
    suspend fun getCount(): Int
}
