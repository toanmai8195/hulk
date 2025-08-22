package com.tm.kotlin.service.counter

import com.tm.kotlin.common.hbase.HBaseClientFactory
import com.tm.kotlin.common.hbase.HBaseFactory
import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.common.mods.monitor.MMonitor
import com.tm.kotlin.service.counter.verticles.friendbybitmap.VSizeBitmapTest
import com.tm.kotlin.service.counter.verticles.friendbycell.VAddFriendByCell
import com.tm.kotlin.service.counter.verticles.friendbycell.VCheckFriendByCell
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

    @Provides
    @IntoMap
    @StringKey(value = "VAddFriendByCell")
    fun provideVAddFriendByCell(verticle: VAddFriendByCell): Verticle {
        return verticle
    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VAddFriendBitMap")
//    fun provideVAddFriendBitMap(verticle: VAddFriendBitMap): Verticle {
//        return verticle
//    }
//
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetTotalFriendBitMap")
//    fun provideVGetTotalFriendBitMap(verticle: VGetTotalFriendBitMap): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetTotalFriendByCell")
//    fun provideVGetTotalFriendByCell(verticle: VGetTotalFriendByCell): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetTotalMutualFriendBitmap")
//    fun provideVGetTotalMutualFriendBitmap(verticle: VGetTotalMutualFriendBitmap): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetTotalMutualFriendCell")
//    fun provideVGetTotalMutualFriendCell(verticle: VGetTotalMutualFriendCell): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetMutualFriendCell")
//    fun provideVGetMutualFriendCell(verticle: VGetMutualFriendCell): Verticle {
//        return verticle
//    }

//    @Provides
//    @IntoMap
//    @StringKey(value = "VGetMutualFriendBitmap")
//    fun provideVGetMutualFriendBitmap(verticle: VGetMutualFriendBitmap): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VCheckFriendByCell")
//    fun provideVCheckFriendByCell(verticle: VCheckFriendByCell): Verticle {
//        return verticle
//    }
//
//    @Provides
//    @IntoMap
//    @StringKey(value = "VCheckFriendBitMap")
//    fun provideVCheckFriendBitMap(verticle: VCheckFriendBitMap): Verticle {
//        return verticle
//    }

//    @Provides
//    @IntoMap
//    @StringKey(value = "VSizeBitmapTest")
//    fun provideVSizeBitmapTest(verticle: VSizeBitmapTest): Verticle {
//        return verticle
//    }
}