package com.tm.kotlin.service.counter

import com.tm.kotlin.common.hbase.HBaseClientFactory
import com.tm.kotlin.common.hbase.HBaseFactory
import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.verticles.VAddFriend
import com.tm.kotlin.service.counter.verticles.VGetFriend
import com.tm.kotlin.service.counter.verticles.VGetMutualFriend
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
        HBaseClientFactory::class
    ]
)
class ServiceModule {
    companion object {
        private const val TABLE_NAME = "hulk:com.tm.counter"
    }

    @Provides
    @Singleton
    fun provideHbaseClient(hBaseFactory: HBaseFactory): IHBaseClient {
        return hBaseFactory.create(TABLE_NAME)
    }

    @Provides
    @Singleton
    fun provideHBaseConfig(): Configuration {
        val config: Configuration = HBaseConfiguration.create()
        config.set("hbase.zookeeper.quorum", "hbase")
        config.set("hbase.zookeeper.property.clientPort", "2182")
        return config
    }

//    @Provides
//    @IntoMap
//    @StringKey(value = "VCounter")
//    fun provideVCounter(verticle: CounterVerticle): Verticle {
//        return verticle
//    }

//    @Provides
//    @IntoMap
//    @StringKey(value = "VAddFriend")
//    fun provideVAddFriend(verticle: VAddFriend): Verticle {
//        return verticle
//    }

//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetFriend")
//    fun provideVGetFriend(verticle: VGetFriend): Verticle {
//        return verticle
//    }

    @Provides
    @IntoMap
    @StringKey(value = "VGetMutualFriend")
    fun provideVGetMutualFriend(verticle: VGetMutualFriend): Verticle {
        return verticle
    }
}