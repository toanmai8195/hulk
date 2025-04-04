package com.tm.java.consumer;

import com.tm.java.common.module.BaseModule;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;

import javax.inject.Singleton;

@Module(includes = {BaseModule.class})
public class ConsumerModule {
    @Provides
    @Singleton
    Vertx provideVertx() {
        return Vertx.vertx();
    }
}
