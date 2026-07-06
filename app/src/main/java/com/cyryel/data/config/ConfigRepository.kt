package com.cyryel.data.config

interface ConfigRepository {
    suspend fun getBankAccounts(): Result<List<BankAccountData>>
}
