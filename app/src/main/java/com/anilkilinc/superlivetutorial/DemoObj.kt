package com.anilkilinc.superlivetutorial

class DemoObj constructor(test:ITest) {
    var value = 7

    init {
        value = test.doThat()
    }
}