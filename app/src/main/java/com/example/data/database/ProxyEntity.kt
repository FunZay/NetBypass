package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_servers")
data class ProxyEntity(
    @PrimaryKey val id: String, // "ip:port" to ensure uniqueness
    val ip: String,
    val port: Int,
    val protocol: String, // "SOCKS5" or "HTTP"
    val country: String, // Country name or code
    val latencyMs: Long, // Latency in milliseconds (-1 for offline)
    val lastChecked: Long, // Timestamp of last check
    val isActive: Boolean // Verified active state
)
