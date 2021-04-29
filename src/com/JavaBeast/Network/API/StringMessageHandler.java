package com.JavaBeast.Network.API;

import java.net.Socket;

public interface StringMessageHandler {

    void onClientReceive(String prefix, String message, String[] args);
    void onServerReceive(String prefix, String message, Socket sender, String[] args);

}
