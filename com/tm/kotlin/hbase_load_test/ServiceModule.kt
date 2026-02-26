package com.tm.kotlin.hbase_load_test

import com.tm.kotlin.common.hbase.HBaseClientFactory
import com.tm.kotlin.common.hbase.HBaseFactory
import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.common.mods.monitor.MMonitor
import com.tm.kotlin.hbase_load_test.verticles.RandomRangeLoadTestVerticle
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import javax.inject.Singleton

@Module(
    includes = [
        HBaseClientFactory::class,
        MMonitor::class,
    ]
)
class ServiceModule {
    companion object {
        const val TABLE_NAME = "hulk:load_test"
        const val COLUMN_FAMILY = "cf"
    }

    @Provides
    @Singleton
    fun provideHBaseConfig(): Configuration {
        val config: Configuration = HBaseConfiguration.create()
        config.set("hbase.zookeeper.quorum", "hbase")
        config.set("hbase.zookeeper.property.clientPort", "2181")
        return config
    }

    @Provides
    @Singleton
    fun provideHbaseClient(hBaseFactory: HBaseFactory): IHBaseClient {
        return hBaseFactory.create(TABLE_NAME)
    }

//    @Provides
//    @IntoMap
//    @StringKey(value = "DataCreatorVerticle")
//    fun provideDataCreatorVerticle(verticle: DataCreatorVerticle): Verticle {
//        return verticle
//    }

    @Provides
    @IntoMap
    @StringKey(value = "RandomRangeLoadTestVerticle")
    fun provideRandomRangeLoadTestVerticle(verticle: RandomRangeLoadTestVerticle): Verticle {
        return verticle
    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "FixedStartLoadTestVerticle")
//    fun provideFixedStartLoadTestVerticle(verticle: FixedStartLoadTestVerticle): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "FixedEndLoadTestVerticle")
//    fun provideFixedEndLoadTestVerticle(verticle: FixedEndLoadTestVerticle): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "RandomSubsetLoadTestVerticle")
//    fun provideRandomSubsetLoadTestVerticle(verticle: RandomSubsetLoadTestVerticle): Verticle {
//        return verticle
//    }
}
