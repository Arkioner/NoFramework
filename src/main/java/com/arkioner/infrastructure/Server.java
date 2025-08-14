package com.arkioner.infrastructure;

import com.arkioner.infrastructure.ConfigLoader.ServerProperties;

public class Server {
    public Server(ServerProperties serverProperties) {
        System.out.println("Server Starting with properties: " + serverProperties);
    }
}
