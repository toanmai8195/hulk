package com.tm.java.consumer;

public class Main {
    public static void main(String[] args) {
        ConsumerComponent component = DaggerConsumerComponent.create();
        component.getDeploymentService().start();
    }
}
