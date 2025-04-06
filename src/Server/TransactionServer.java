package Server;

import PageManager.MemoryManager;
import PageManager.PageManager;
import TableManager.TableManager;
import TableManager.QueryToAction;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * Name of class: TransactionServer
 * <p>
 * Description: Takes connections from clients.
 * <p>
 * Version: 1.0
 * <p>
 * Date 04/06
 * <p>
 * Copyright: Semenov Egor
 */

public class TransactionServer {
    private final int port;
    private final int clientHandlerThreads;
    private final int executorThreadsNumber;
    private final TransactionBuffer buffer;
    private ServerSocket serverSocket;
    private ExecutorService clientHandlerPool;
    private List<TransactionExecutor> executors;
    private List<Thread> executorThreads;
    private final PageManager pageManager;

    public TransactionServer(int port, int clientHandlerThreads, int executorThreadsNumber, PageManager pageManager) {
        this.port = port;
        this.clientHandlerThreads = clientHandlerThreads;
        this.executorThreadsNumber = executorThreadsNumber;
        this.buffer = new TransactionBuffer();
        this.pageManager = pageManager;
    }

    public void start() throws IOException {
        // Creating a server socket
        serverSocket = new ServerSocket(port);
        System.out.println("Server is running on port " + port);

        // Initialization of thread pool for client processing
        clientHandlerPool = Executors.newFixedThreadPool(clientHandlerThreads);

        // Initialization of transaction executors
        executors = new ArrayList<>(this.executorThreadsNumber);
        executorThreads = new ArrayList<>(this.executorThreadsNumber);

        for (int i = 0; i < executorThreadsNumber; i++) {
            TransactionExecutor executor = new TransactionExecutor(buffer, new QueryToAction(new TableManager(this.pageManager)));
            executors.add(executor);
            Thread executorThread = new Thread(executor);
            executorThreads.add(executorThread);
            executorThread.start();
        }

        // Accepting client connections
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress());
                clientHandlerPool.execute(new ClientHandler(clientSocket, buffer));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Error accepting the connection: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            clientHandlerPool.shutdown();
            clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS);

            for (TransactionExecutor executor : executors) {
                executor.stop();
            }

            for (Thread thread : executorThreads) {
                thread.join(1000);
            }
        } catch (Exception e) {
            System.err.println("Error when stopping the server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String dataPath = "finalVersion.ehh";
        MemoryManager memoryManager = new MemoryManager(100, dataPath);
        PageManager pageManager = new PageManager(memoryManager);

        int port = 8080;
        int clientHandlers = 10;
        int executors = 4;

        TransactionServer server = new TransactionServer(port, clientHandlers, executors, pageManager);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Error when starting the server: " + e.getMessage());
        }
    }
}
