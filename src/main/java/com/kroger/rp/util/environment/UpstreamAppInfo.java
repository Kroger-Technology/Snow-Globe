package com.kroger.rp.util.environment;

public class UpstreamAppInfo {

    private final String containerName;
    private final int containerPort;

    public UpstreamAppInfo(String containerName, int containerPort) {
        this.containerName = containerName;
        this.containerPort = containerPort;
    }

    public String containerName() {
        return containerName;
    }

    public int port() {
        return containerPort;
    }
}
