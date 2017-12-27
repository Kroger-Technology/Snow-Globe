/*
 * Snow-Globe
 *
 * Copyright 2017 The Kroger Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kroger.oss.snowGlobe.environment;

/**
 * A POJO that is used to hold the container name and port of a "fake" upstream service that
 * will be injected into the Nginx RP configuration.  This is mainly used with the
 * <code>NginxEnvironmentFileBuilder</code> class to build the new upstream file
 *
 */
public class UpstreamAppInfo {

    private final String containerName = "upstream";
    private int containerPort;

    /**
     * The name of the container used in the Docker Compose file to identify the upstream
     * instance.
     * @return
     *      The name of the container that is identified in the docker compose file.
     */
    public String containerName() {
        return containerName;
    }

    /**
     * The named port that traffic should be routed to.
     * @return
     *      The port number to use when communicating with the container.
     */
    public int port() {
        return containerPort;
    }

    public void setPort(int port) {
        containerPort = port;
    }
}
