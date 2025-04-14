package com.tm.kotlin.common.hbase

import dagger.Component
import dagger.Module
import dagger.Provides
import io.vertx.core.Future
import io.vertx.core.Promise
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Increment
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Singleton


@Singleton
@Component(
    modules = [AppModule::class]
)
interface AppComponent {
    val hBaseClient: IHBaseClient
}

@Module(
    includes = [HBaseClientFactory::class]
)
class AppModule {
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

}

fun main() {
    val component = DaggerAppComponent.create()
    val hBaseClient = component.hBaseClient
    try {
        val label = "label14"

        Future.all(
            (1..100).map {
                count(label, hBaseClient)
            }
        ).onComplete {
            println(
                "Counter = ${
                    Bytes.toLong(
                        hBaseClient.get(
                            Get(
                                label.toByteArray()
                            )
                        ).getValue(
                            "df".toByteArray(),
                            "counter".toByteArray()
                        )
                    )
                }"
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun count(label: String, hBaseClient: IHBaseClient): Future<Unit> {
    val promise = Promise.promise<Unit>()
    Thread {
        (1..5000).forEach {
            println("Start counter=${it}: $label in thread=${Thread.currentThread().name}")
            try {
                hBaseClient.inc(
                    Increment(
                        label.toByteArray()
                    ).addColumn(
                        "df".toByteArray(),
                        "counter".toByteArray(),
                        1
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        promise.complete()
    }.start()
    return promise.future()
}


