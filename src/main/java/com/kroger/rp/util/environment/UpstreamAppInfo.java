package com.kroger.rp.util.environment;

/**
 * A POJO that is used to hold the container name and port of a "fake" upstream service that
 * will be injected into the Nginx RP configuration.  This is mainly used with the
 * <code>NginxEnvironmentFileBuilder</code> class to build the new upstream file
 *
 */
public class UpstreamAppInfo {

    private final String containerName;
    private final int containerPort;

    /**
     * Constructor to build the POJO.
     *
     * @param containerName
     *      The name of the container used in the Docker Compose file to identify the upstream
     *      instance.
     * @param containerPort
     *      The named port that the traffic should be routed to.
     */
    public UpstreamAppInfo(String containerName, int containerPort) {
        this.containerName = containerName;
        this.containerPort = containerPort;
    }

    /**
     * The name of the container used in the Docker Compose file to identify the upstream
     * instance.
     */
    public String containerName() {
        return containerName;
    }

    /**
     * The named port that traffic should be routed to.
     * @return
     */
    public int port() {
        return containerPort;
    }
}
