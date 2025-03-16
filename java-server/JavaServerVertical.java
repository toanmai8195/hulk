package com.tm;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class JavaServerVertical extends AbstractVerticle {
    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> {
            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello, World!");
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        System.out.println("HTTP server started on port 8080");
                    } else {
                        System.out.println("Failed to start HTTP server: " + http.cause().getMessage());
                    }
                });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new JavaServerVertical());
    }
}
