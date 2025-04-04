package com.tm.java.common.module;

import com.tm.java.common.service.monitor.MMonitor;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(
        includes = {
                BindingModule.class,
                MMonitor.class
        }
)
public class BaseModule {
    @Provides
    @Named("a")
    String provideText() {
        return "toan";
    }
}


