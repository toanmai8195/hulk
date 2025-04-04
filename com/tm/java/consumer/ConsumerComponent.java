package com.tm.java.consumer;

import dagger.Component;

import com.tm.java.common.service.deployment.DeploymentService;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ConsumerModule.class})
interface ConsumerComponent {
    DeploymentService getDeploymentService();
}
