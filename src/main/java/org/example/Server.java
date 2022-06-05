package org.example;

import com.google.common.primitives.Bytes;
import lombok.Setter;
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
@Setter
public class Server {
    public static final byte[] CRLFCRLF = {'\r', '\n', '\r', '\n'};
    private int soTimeout = 30 * 1000;
    private  int readTimeout = 60 * 1000;
    private  int bufferSize = 4096;
    private int port = 9999;

    public void start(){
        try (
               final ServerSocket serverSocket = new ServerSocket(port)
        ) {
            while (true) {
                try {
                    final Socket socket = serverSocket.accept();
                    handleClient(socket);
                } catch(Exception e){ //если произошла проблема с клиентом
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {  //если не удалось запустить сервер
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) throws IOException {
        socket.setSoTimeout(soTimeout);

        try (
                socket; //
                final OutputStream out = socket.getOutputStream();
                final InputStream in = socket.getInputStream();
        ) {
            System.out.println(socket.getInetAddress());
            out.write("Enter command\n".getBytes(StandardCharsets.UTF_8));

            //внутренний цикл
            final String message = readMessage(in);
            System.out.println("message = " + message);
            final String response =
                    "HTTP/1.1 200 OK\r\n"+
                            "Connection: close \r\n" +
                            "Content-Lenght: 2\r\n" +
                            "OK";
            out.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
    private String readMessage(final InputStream in) throws IOException, BadRequestException {
        final byte[] buffer = new byte[bufferSize];
        int offset = 0;
        int length = buffer.length;
        final Instant deadline = Instant.now().plusMillis(readTimeout);
        while (true) {
            if (Instant.now().isAfter(deadline)){
                throw new DeadlineExceedException();
            }
            final int read = in.read(buffer, offset, length);
            offset+=read;
            length=buffer.length- offset;
            if (read == 0|| length ==0){
                break;
            }
            final int headersEndIndex = Bytes.indexOf(buffer, CRLFCRLF);
            if (headersEndIndex != -1){
                break;
            }
            if (read==-1){
                throw new BadRequestException("CRLFCRLF not gound, no more data");
            }
            if (length==0){
                throw new BadRequestException("CRLFCRLF not gound");
            }
        }
        final String message = new String(buffer, 0, buffer.length - length, StandardCharsets.UTF_8).trim();
        return message;
    }
}
