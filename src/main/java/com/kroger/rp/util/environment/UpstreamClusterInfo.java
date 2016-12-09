package com.kroger.rp.util.environment;

import java.util.List;

public class UpstreamClusterInfo {

    private final String clusterName;
    private final List<UpstreamAppInfo> appInstances;

    public UpstreamClusterInfo(String clusterName, List<UpstreamAppInfo> appInstances) {
        this.clusterName = clusterName;
        this.appInstances = appInstances;
    }

    public List<UpstreamAppInfo> getAppInstances() {
        return appInstances;
    }

    public String getClusterName() {
        return clusterName;
    }
}
