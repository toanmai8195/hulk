package com.tm.java.common.service.monitor;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import io.vertx.core.Verticle;

import com.tm.java.common.annotation.VerticleMapKey;

@Module
public class MMonitor {
    @Provides
    @IntoMap
    @VerticleMapKey(MonitoringVerticle.class)
    Verticle provideMonitoringVerticle(MonitoringVerticle vertical) {
        return vertical;
    }
}
