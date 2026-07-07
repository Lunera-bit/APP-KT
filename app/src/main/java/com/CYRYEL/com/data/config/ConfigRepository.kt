package com.CYRYEL.com.data.config

interface ConfigRepository {
    suspend fun getBankAccounts(): Result<List<BankAccountData>>
    suspend fun getDeliveryConfig(): Result<DeliveryConfigData>
}
