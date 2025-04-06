import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Server extends Thread{
    private QueryBuffer queryBuffer;
    private volatile boolean running = true;
    private int port;
    public Server(QueryBuffer queryBuffer, int port){
        this.setName("Server"+this.threadId());
        this.queryBuffer = queryBuffer;
        this.port = port;
    }
    public void run() {
        Interpreter interpreter = new Interpreter();
        QueryBuffer queryBuffer = new QueryBuffer();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                try (Socket clientSocket = serverSocket.accept();
                     InputStream inputStream = clientSocket.getInputStream();
                     ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    String receivedContent = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                    queryBuffer.appendQueryList(interpreter.readScript(receivedContent, false));
                    running = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}