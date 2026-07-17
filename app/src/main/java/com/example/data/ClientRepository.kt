package com.example.data

import kotlinx.coroutines.flow.Flow

class ClientRepository(private val db: AppDatabase) {
    private val clientDao = db.clientDao()

    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    val activeClients: Flow<List<Client>> = clientDao.getActiveClients()

    suspend fun getClientById(id: Long): Client? = clientDao.getClientById(id)

    suspend fun getClientByMobile(mobile: String): Client? = clientDao.getClientByMobile(mobile)

    suspend fun saveClient(client: Client): Long = clientDao.insertClient(client)

    suspend fun updateClient(client: Client) = clientDao.updateClient(client)

    suspend fun deleteClient(client: Client) = clientDao.deleteClient(client)

    suspend fun updateClientStatus(id: Long, isActive: Boolean) {
        clientDao.updateClientStatus(id, isActive)
    }
}
