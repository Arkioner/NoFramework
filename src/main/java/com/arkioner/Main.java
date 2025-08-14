package com.arkioner;

import com.arkioner.infrastructure.ConfigLoader;
import com.arkioner.infrastructure.DIContainer;
import com.arkioner.infrastructure.Server;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) throws Exception {
        DIContainer diContainer = new DIContainer();
        diContainer.register(Server.class);
        diContainer.registerInstance(new ObjectMapper());
        diContainer.registerInstance(ConfigLoader.loadServerConfig(diContainer.resolve(ObjectMapper.class)));
        diContainer.register(Server.class);

        Server server = diContainer.resolve(Server.class);
    }
}