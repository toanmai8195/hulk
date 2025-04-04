package com.tm.java.common.module;

import dagger.Module;
import dagger.multibindings.Multibinds;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;

import java.util.Map;

@Module
public abstract class BindingModule {
    @Multibinds
    abstract Map<Class<?>, Verticle> provideEmptyVerticleMap();

    @Multibinds
    abstract Map<Class<?>, DeploymentOptions> provideEmptyOptionMap();
}
