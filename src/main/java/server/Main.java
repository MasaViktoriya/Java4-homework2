package server;

import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        NioServer nioServer = null;
        try {
            nioServer = new NioServer();
        } catch (IOException e) {
            System.out.println("Server initialization error");
            e.printStackTrace();
        }
        try {
            nioServer.start();
        } catch (IOException e) {
            System.out.println("Network error");
            e.printStackTrace();
        }
    }
}
