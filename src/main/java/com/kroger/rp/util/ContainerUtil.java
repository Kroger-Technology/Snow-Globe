package com.kroger.rp.util;

import java.io.IOException;
import java.net.ServerSocket;

public class ContainerUtil {

    public static int getAvailablePort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int localPort = serverSocket.getLocalPort();
            serverSocket.close();
            return localPort;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
