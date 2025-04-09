package com.tm.kotlin.common.http

import dagger.Binds
import dagger.Module

@Module
interface MHttpFactory {
    @Binds
    fun bindHttpFactory(factory: VHttp.Factory): HttpFactory
}