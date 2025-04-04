package com.tm.java.common.service.monitor;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MonitoringVerticle extends AbstractVerticle {

    private final PrometheusMeterRegistry registry;
    private final Vertx vertx;

    @Inject
    public MonitoringVerticle(
            Vertx vertx
    ) {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        this.vertx = vertx;
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.get("/metrics").handler(rc -> {
            rc.response().putHeader("Content-Type", "text/plain").end(registry.scrape());
        });

        vertx.createHttpServer().requestHandler(router).listen(8080);
    }
}