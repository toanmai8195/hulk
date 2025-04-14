package com.tm.kotlin.common.hbase

import dagger.Binds
import dagger.Module

@Module
interface HBaseClientFactory {
    @Binds
    fun bindHBaseFactory(factory: HBaseClient.Factory): HBaseFactory
}