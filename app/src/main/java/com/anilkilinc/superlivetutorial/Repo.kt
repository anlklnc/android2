package com.anilkilinc.superlivetutorial

class Repo(private val api:DemoAPI){

    suspend fun getItems():List<Item>? {
        return api.getItems().body()
    }
}