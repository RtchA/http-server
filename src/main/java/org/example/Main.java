package org.example;


import com.google.common.primitives.Bytes;
import org.example.exception.BadRequestException;
import org.example.exception.DeadlineExceedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Main {
    public static void main(String[] args) {
        final Server server = new Server();
        server.setPort(7777);
        server.start();
    }
}
