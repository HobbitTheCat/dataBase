package QueryGeneration;

import java.io.*;
import java.net.*;

public class Client extends Thread{
    private int serverPort;
    private String scriptPath;
    public Client(int port, String scriptPath){
        this.serverPort = port;
        this.scriptPath = scriptPath;
    }
    public void run() {
        try (Socket socket = new Socket("localhost", serverPort);
             OutputStream outputStream = socket.getOutputStream();
             FileInputStream fileInputStream = new FileInputStream(scriptPath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}