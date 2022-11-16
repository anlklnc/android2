package com.anilkilinc.superlivetutorial

import javax.inject.Inject

class TestImpl @Inject constructor():ITest {

    override fun doThis() {
        println("do this!")
    }

    override fun doThat(): Int {
        return 2
    }
}