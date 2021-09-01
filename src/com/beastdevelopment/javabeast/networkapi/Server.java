package com.beastdevelopment.javabeast.networkapi;

/*
    All the logic needed for a server.
    Accepting clients & receiving messages as well as sending and broadcasting.

    Make sure to call the 'Server.start()' method, else the server will just exist in the 'serverList'.

    If you want to remove a Server from the 'serverList' you have to call the 'Server.delete()' method.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

    // all servers will be added to this map.
    public static final HashMap<String, Server> serverList = new HashMap<>();

    /*
        the name of the server can be requested by any client.
        The server will be saved in the 'serverList' with the name as key.
     */
    private final String name;

    /*
        The port were the server will run on.
        Make sure to use a port which isn't already in use.
     */
    private final int port;

    // will be set in the 'Server.start()' method.
    private ServerSocket serverSocket;

    // will be created in the 'Server.accept()' method.
    private Thread acceptThread;

    // time to wait between two connections.
    public int CONNECTIONTHROTTLE = 1000;

    // time to wait before reading a new message.
    public int RECEIVETHROTTLE = 100;

    // current clientID
    private int clientID = 0;

    private final HashMap<Integer, Client> clientList = new HashMap<>();

    /*
        if this constructor is called the server will just be added to the
        'serverList'. To start it you also need to run 'server.start()'
     */
    public Server(String name, int port) {
        this.name = name;
        this.port = port;

        // add to the serverList
        serverList.put(name, this);
    }

    /*
        Call to start the server and wait for clients to connect.
     */
    public void start() throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.accept();
    }

    private void accept() {
        Thread thread = new Thread(()->{

            // make sure the serversocket was created.
            if(this.serverSocket != null) {

                try {

                    // loop infinity
                    while (true) {

                        // accept a socket connection.
                        Socket socket = this.serverSocket.accept();
                        int socketID = this.clientID;

                        this.clientID++;

                        // create a new client and add it to the client list.
                        Client client = new Client(socket, clientID, RECEIVETHROTTLE);
                        clientList.put(clientID, client);

                        // TODO: trigger connect event (client, clientID)

                        // wait before accepting a new socket.
                        Thread.sleep(this.CONNECTIONTHROTTLE);

                    }

                }catch (Exception exception) { }
            }
        });

        this.acceptThread = thread;
        thread.start();
    }

    /*
        Close the serversocket.
     */
    public void stop() throws IOException {
        if(this.serverSocket != null) {
            this.serverSocket.close();
        }
    }

    /*
        Stops the server and removes it from the 'serverList'.
     */
    public void delete() throws IOException {

        // make sure the server is stopped.
        this.stop();

        // remove the server from the 'serverList'.
        serverList.remove(this.name);
    }

    public String getName() {
        return this.name;
    }

    public int getPort() {
        return this.port;
    }

}
