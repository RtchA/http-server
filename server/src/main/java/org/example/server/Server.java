package org.example.server;

import com.google.common.primitives.Bytes;
import lombok.Setter;
import org.example.server.exception.BadRequestException;
import org.example.server.exception.DeadlineExceedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Struct;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Setter
public class Server {
    public static final byte[] CRLFCRLF = {'\r', '\n', '\r', '\n'};

    public static final byte[] CRLF = {'\r', '\n'};
    private int soTimeout = 30 * 1000;
    private  int readTimeout = 60 * 1000;
    private  int bufferSize = 4096;
    private int port = 9999;

    private Map<String,Handler> routes = new HashMap<>();

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

    private void handleClient(Socket socket) throws Exception {

        try (
                socket; //
                final OutputStream out = socket.getOutputStream();
                final InputStream in = socket.getInputStream();
        ) {
            socket.setSoTimeout(soTimeout);
            System.out.println(socket.getInetAddress());
            //внутренний цикл
            final Request request = readRequest(in);
            System.out.println("Request" + request);

            Handler handler = routes.get(request.getPath());
            if (handler == null) {
                final String response =
                        "HTTP/1.1 404 Not Found\r\n"+
                                "Connection: close\r\n" +
                                "Content-Length: 9\r\n\r\n" +
                                "Not Found";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                return;
            }

            handler.handle(request, out);
            }

    }
    private Request readRequest(final InputStream in) throws IOException, BadRequestException {
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
        final Request request = new Request();
        final int requestLineEndIndex = Bytes.indexOf(buffer, CRLF);
        if (requestLineEndIndex==-1){
            throw new BadRequestException ("Request Line Not Found");
        }
        final String requestLine = new String(buffer, 0, requestLineEndIndex, StandardCharsets.UTF_8);
        System.out.println("requestLine = " + requestLine);

        final String[] parts = requestLine.split(" ");
        request.setMetod(parts[0]);
        request.setPath(parts[1]);

        return request;
    }

    public void register(String path, Handler handler) {
        routes.put (path, handler);
    }
}
