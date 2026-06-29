package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.PortParser
import kotlinx.serialization.Serializable

@Entity(tableName = "servers")
@Serializable
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ip: String,
    val ports: String = "22",
    val countryCode: String = "",
    val username: String = "root",
    val password: String = ""
) {
    /** First configured port for display when no active session port is known. */
    val displayPort: Int
        get() = PortParser.parsePorts(ports).firstOrNull() ?: 22
}
