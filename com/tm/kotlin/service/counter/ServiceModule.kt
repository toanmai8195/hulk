package com.tm.kotlin.service.counter

import com.tm.kotlin.common.hbase.HBaseClientFactory
import com.tm.kotlin.common.hbase.HBaseFactory
import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.common.mods.monitor.MMonitor
import com.tm.kotlin.service.counter.dao.HBaseCounterDao
import com.tm.kotlin.service.counter.dao.ICounterDao
import com.tm.kotlin.service.counter.handler.ChatCounterHandler
import com.tm.kotlin.service.counter.handler.StatefulCounterHandler
import com.tm.kotlin.service.counter.handler.StatelessCounterHandler
import com.tm.kotlin.service.counter.verticles.api.VCounterHttpApiV2
import com.tm.kotlin.service.counter.verticles.consumer.VChatCounterConsumer
import com.tm.kotlin.service.counter.verticles.consumer.VStatefulCounterConsumerV2
import com.tm.kotlin.service.counter.verticles.consumer.VStatelessCounterConsumerV2
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*
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
        private const val KAFKA_SERVERS = "kafka:9092"
    }

    // ===== DATABASE LAYER =====
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
        config.set("hbase.zookeeper.property.clientPort", "2181")
        return config
    }

    @Provides
    @Singleton
    fun provideCounterDao(hbaseClient: IHBaseClient): ICounterDao {
        return HBaseCounterDao(hbaseClient)
    }

    // ===== CONFIGURATION =====
    @Provides
    @Singleton
    fun provideStatelessCounterConfig(): CounterConfig {
        return CounterConfig(
            tableName = TABLE_NAME,
            columnFamily = "stateless",
            allowNegative = false,
            maxValue = 999999L
        )
    }

    @Provides
    @Singleton
    @StatefulConfig
    fun provideStatefulCounterConfig(): CounterConfig {
        return CounterConfig(
            tableName = TABLE_NAME,
            columnFamily = "stateful",
            allowNegative = false,
            maxValue = null
        )
    }

    @Provides
    @Singleton
    fun provideKafkaConsumerProperties(): Properties {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = KAFKA_SERVERS
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return props
    }

    // ===== HANDLER LAYER =====
    @Provides
    @Singleton
    fun provideStatelessCounterHandler(
        counterDao: ICounterDao,
        config: CounterConfig
    ): StatelessCounterHandler {
        return StatelessCounterHandler(counterDao, config)
    }

    @Provides
    @Singleton
    fun provideStatefulCounterHandler(
        counterDao: ICounterDao,
        @StatefulConfig config: CounterConfig
    ): StatefulCounterHandler<String> {
        return StatefulCounterHandler(
            counterDao = counterDao,
            config = config,
            itemSerializer = { it },
            itemDeserializer = { it }
        )
    }

    @Provides
    @Singleton
    fun provideChatCounterHandler(
        statefulHandler: StatefulCounterHandler<String>
    ): ChatCounterHandler {
        return ChatCounterHandler(statefulHandler)
    }

    // ===== VERTICLE LAYER =====
    @Provides
    @IntoMap
    @StringKey(value = "VStatelessCounterConsumerV2")
    fun provideVStatelessCounterConsumerV2(verticle: VStatelessCounterConsumerV2): Verticle {
        return verticle
    }

    @Provides
    @IntoMap
    @StringKey(value = "VStatefulCounterConsumerV2")
    fun provideVStatefulCounterConsumerV2(verticle: VStatefulCounterConsumerV2): Verticle {
        return verticle
    }

    @Provides
    @IntoMap
    @StringKey(value = "VChatCounterConsumer")
    fun provideVChatCounterConsumer(verticle: VChatCounterConsumer): Verticle {
        return verticle
    }

    @Provides
    @IntoMap
    @StringKey(value = "VCounterHttpApiV2")
    fun provideVCounterHttpApiV2(verticle: VCounterHttpApiV2): Verticle {
        return verticle
    }

    // ===== QUALIFIERS =====
    @javax.inject.Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class StatefulConfig
}