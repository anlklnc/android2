package com.anilkilinc.superlivetutorial

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object AppModule {

    @Provides
    fun provideDemo(test:ITest):DemoObj {
        return DemoObj(test)
    }
}

@Module
@InstallIn(ActivityComponent::class)
abstract class InterfaceModule {
    @Binds
    abstract fun provideTest(test:TestImpl):ITest
}