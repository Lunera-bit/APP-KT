package com.cyryel.data.order

interface OrderRepository {
    suspend fun createOrder(request: CreateOrderRequest): Result<String>
    suspend fun getOrdersByUserId(userId: String): Result<List<Order>>
    suspend fun getOrderById(orderId: String): Result<Order>
    suspend fun getOrdersByUserIdPaginated(
        userId: String,
        lastTimestamp: Long? = null,
        pageSize: Int = 20
    ): Result<Pair<List<Order>, Boolean>>
}
