package com.example.data

class ServerRepository(private val dao: ServerDao) {
    val allServers = dao.getAllServers()

    suspend fun insert(server: ServerEntity) = dao.insertServer(server)
    suspend fun update(server: ServerEntity) = dao.updateServer(server)
    suspend fun delete(server: ServerEntity) = dao.deleteServer(server)
    suspend fun getById(id: Int) = dao.getServerById(id)
}
