package com.tm.java.service.grpc.service;

import com.tm.abc.EventRequest;

public class GrpcTest {
    public static void main(String[] args) {
        EventRequest eventRequest = EventRequest.newBuilder().setName("toanmai").build();
        System.out.println(eventRequest.getName());
    }
}
