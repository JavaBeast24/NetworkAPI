package com.JavaBeast.Network.API;

import java.net.Socket;

public interface ByteMessageHandler {

    void onClientReceive(String prefix, byte[] bytes, String info);
    void onServerReceive(String prefix, byte[] bytes, Socket sender, String info);

}
