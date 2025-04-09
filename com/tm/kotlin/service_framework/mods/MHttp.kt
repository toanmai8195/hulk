package com.tm.kotlin.service_framework.mods

import com.tm.kotlin.common.http.API
import com.tm.kotlin.common.http.HttpFactory
import com.tm.kotlin.common.http.MHttpFactory
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import io.vertx.core.Verticle
import io.vertx.core.http.HttpMethod

@Module(
    includes = [
        MHttpFactory::class
    ]
)
class MHttp {
    @Provides
    @IntoMap
    @StringKey(value = "HelloService")
    fun provideHelloServiceVerticle(httpFactory: HttpFactory): Verticle {
        return httpFactory.create(
            "HelloService",
            1995,
            listOf(
                API(
                    HttpMethod.GET,
                    "/hello"
                ) { ctx ->
                    ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Hello from tm.com")
                },
                API(
                    HttpMethod.POST,
                    "/say-hello"
                ) { ctx ->
                    val body = ctx.body().asJsonObject()
                    val name = body.getString("name") ?: "UNKNOWN"
                    ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Hello $name")

                }
            )
        )
    }

    @Provides
    @IntoMap
    @StringKey(value = "GoodbyeService")
    fun provideGoodbyeServiceServiceVerticle(httpFactory: HttpFactory): Verticle {
        return httpFactory.create(
            "GoodbyeService",
            1998,
            listOf(
                API(
                    HttpMethod.GET,
                    "/goodbye"
                ) { ctx ->
                    ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Goodbye from tm.com")
                },
                API(
                    HttpMethod.POST,
                    "/say-goodbye"
                ) { ctx ->
                    val body = ctx.body().asJsonObject()
                    val name = body.getString("name") ?: "UNKNOWN"
                    ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Goodbye $name")

                }
            )
        )
    }

    @Provides
    @IntoMap
    @StringKey(value = "GoodbyeService")
    fun provideMyServiceVerticleOption(): DeploymentOptions {
        return DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER)
            .setInstances(2)
    }
}