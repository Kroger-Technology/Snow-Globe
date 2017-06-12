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

package com.kroger.oss.snowGlobe.util;

import java.io.*;

public class DockerNetworking {

    public static final String SNOW_GLOBE_NETWORK = "snow-globe";

    public static void createNetwork() {
        if (!networkExists()) {
            makeNetwork();
        }
    }

    private static void makeNetwork() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "network", "create", SNOW_GLOBE_NETWORK);
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean networkExists() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "network", "ls");
            Process process = processBuilder.start();
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (reader.ready()) {
                if (reader.readLine().contains(SNOW_GLOBE_NETWORK)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
