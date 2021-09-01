package com.beastdevelopment.javabeast.networkapi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {

    private final Socket socket;
    private int clientID;
    private Thread receive;
    private final long receiveThrottle;
    private final boolean serverSide;


    Client(Socket socket, int clientID, long receiveThrottle) {
        this.socket = socket;
        this.clientID = clientID;
        this.receiveThrottle = receiveThrottle;
        this.serverSide = true;
        this.receive();
    }

    public Client(long receiveThrottle, String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.receiveThrottle = receiveThrottle;
        this.serverSide = false;
        this.clientID = 0;
        this.receive();
    }

    private void receive() {
        Thread thread = new Thread(()->{
            try {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                while(true) {

                    if(inputStream.available() > 0) {
                        byte[] bytes = new byte[inputStream.available()];
                        inputStream.read(bytes);

                        String string = new String(bytes);

                        String[] strings = string.split("//END//");

                        for (String s : strings) {
                            Package pack = Package.ofString(s);

                            if (this.serverSide) {
                                pack.onServerReceive(this, pack);
                            } else
                                pack.onClientReceive(pack);

                        }

                    }

                    Thread.sleep(this.receiveThrottle);
                }

            }catch (Exception ignored) { }
        });

        this.receive = thread;
        thread.start();
    }

    public void send(Package pack) throws Exception {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.write(pack.toNetworkString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

}
