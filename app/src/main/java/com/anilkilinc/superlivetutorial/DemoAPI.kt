package com.anilkilinc.superlivetutorial

import retrofit2.Response
import retrofit2.http.GET

interface DemoAPI {

    @GET("/todos")
    suspend fun getItems():Response<List<Item>>
}