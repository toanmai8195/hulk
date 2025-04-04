package com.tm.java.common.service.deployment;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeploymentService {
    Map<Class<?>, Verticle> verticals;
    Map<Class<?>, DeploymentOptions> options;
    Vertx vertx;
    boolean isDeployed = false;

    @Inject
    public DeploymentService(
            Map<Class<?>, Verticle> initVerticals,
            Map<Class<?>, DeploymentOptions> initOptions,
            Vertx vertx
    ) {
        verticals = initVerticals;
        options = initOptions;
        this.vertx = vertx;
    }

    public void start() {
        System.out.println("[Deploy] Starting ...");
        List<Future<String>> futures = verticals.entrySet().stream()
                .map(entry -> {
                    DeploymentOptions option = options.getOrDefault(entry.getKey(), new DeploymentOptions());
                    return vertx.deployVerticle(entry.getValue(), option);
                }).collect(Collectors.toList());

        Future.all(futures).onComplete(rs -> {
            isDeployed = true;
            if (rs.succeeded()) {
                System.out.println("[Deploy] Readied!");
            } else {
                System.out.println("[Deploy] Failed! {" + rs.cause().getMessage() + "}");
                rs.cause().printStackTrace();
            }
        });
    }
}
