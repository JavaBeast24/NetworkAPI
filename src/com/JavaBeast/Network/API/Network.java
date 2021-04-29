package com.JavaBeast.Network.API;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Network {

    // the client which will be set to a Socket while connected to a server,
    // else it will be null
    private static Socket client = null;

    // the thread were the client will receive its messages
    private static Thread clientReceiveThread = null;

    // true if the client is currently receiving messages
    private static boolean clientIsReceiving = false;

    // the server which will be set to a ServerSocket while a server is running,
    // else it will be null
    private static ServerSocket server = null;

    // the thread were the server will receive its messages
    private static Thread serverReceiveThread = null;

    // true if the server is currently receiving messages
    private static boolean serverIsReceiving = false;



    // all StringMessageHandlers registered with their prefix
    private static HashMap<String, StringMessageHandler> stringMessageHandlers = new HashMap<>();

    // all StringMessageHandlers registered with their prefix
    private static HashMap<String, ByteMessageHandler> byteMessageHandlers = new HashMap<>();



    /*
    /**
     * @param ip the ip of your server
     * @param port the port of your server
     * @return error code : 0 = success; 1 = already connected to a server; 2 = connection error;
     */
    public static int Connect(String ip, int port) {

        if(Network.client == null){

            try{

                // try to create new Socket
                client = new Socket(ip, port);

                // start to receive
                ClientReceive();

                return 0;

            }catch(IOException ioException){

                return 2;

            }
        }else {

            return 1;

        }

    }

    /**
     * @param port the port were the server will run on
     * @return error code : 0 = success; 1 = server already running; 2 = error while creating (maybe port already used)
     */
    public static int Open(int port) {

        // check if there is already a server running
        if(Network.server == null){

            try {

                // try to create new server
                Network.server = new ServerSocket(port);

                // return 0 (success)
                return 0;

            } catch (IOException ioException) {

                // return error code 2 if creating failed
                // maybe because port is already used
                return 2;

            }

        } else {

            // return 1 (server already created)
            return 1;

        }

    }




    // starts the clientReceiver
    private static void ClientReceive() {

        Thread thread = new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        clientIsReceiving = true;


                        DataInputStream inputStream = null;

                        try {

                            // try to create a new inputStream to read bytes from
                            inputStream = new DataInputStream(client.getInputStream());


                        } catch (IOException ioException) {

                            // set clientIsReceiving to false to prevent from starting with out a inputStream
                            clientIsReceiving = false;

                            // close the client if the inputStream creation failed
                            CloseClient();

                        }

                        // infinity loop to read messages
                        while ( clientIsReceiving ) {

                            try {

                                // check if we got a new input
                                if (inputStream.available() > 0) {

                                    // read all bytes from stream
                                    byte[] inputBytes = new byte[inputStream.available()];
                                    inputStream.read(inputBytes);

                                    // create new String from inputBytes
                                    String inputString = new String(inputBytes);

                                    // split messages on "/end/" to check for multiple messages
                                    String[] inputStrings = inputString.split("/end/");

                                    // loop through all received messages
                                    for ( String input : inputStrings) {

                                        // check if received messages is string
                                        if ( !input.startsWith("/bytes/") ) {

                                            // split up current string (example: 'myPrefix/prefend/my message')
                                            // -> [0] = 'myPrefix'
                                            // -> [1] = 'my message'
                                            String[] inputData = input.split("/prefend/");

                                            // get prefix from inputData
                                            String prefix = inputData[0];

                                            // split up message into arguments
                                            String[] arguments = inputData[1].split(" ");

                                            // check if there is a stringMessageHandler existing with that prefix
                                            if ( stringMessageHandlers.containsKey( prefix ) ) {

                                                // trigger the stringMessageHandler
                                                stringMessageHandlers.get( prefix ).onClientReceive( prefix, inputData[1], arguments);

                                            }

                                        } else {

                                            // remove byte tag from input
                                            input = input.replace("/bytes/", "");

                                            // split up current string (example: 'myPrefix/prefend/custom info/infoend/myBytes')
                                            // -> [0] = 'myPrefix'
                                            // -> [1] = 'my message'
                                            String[] inputData = input.split("/prefend/");

                                            // get prefix from inputData
                                            String prefix = inputData[0];

                                            // split up current string (example: 'custom info/infoend/myBytes')
                                            inputData = inputData[1].split("/infoend/");

                                            // get info from inputData
                                            String info = inputData[0];

                                            // turn message back to bytes
                                            inputBytes = inputData[1].getBytes(StandardCharsets.UTF_8);

                                            // check if there is a byteMessageHandler existing with that prefix
                                            if ( byteMessageHandlers.containsKey( prefix ) ) {

                                                // trigger the stringMessageHandler
                                                byteMessageHandlers.get( prefix ).onClientReceive( prefix, inputBytes, info);

                                            }

                                        }

                                    }

                                } else {

                                    // check if client is still connected
                                    if ( !client.isConnected() || client.isClosed() || !client.isBound() ) {

                                        // set clientIsReceiving to false to prevent from starting with out a inputStream
                                        clientIsReceiving = false;

                                        // close the client if the inputStream creation failed
                                        CloseClient();

                                    }

                                }


                            } catch ( IOException ioException ) {

                                // set clientIsReceiving to false to prevent from starting with out a inputStream
                                clientIsReceiving = false;

                                // close the client if the inputStream creation failed
                                CloseClient();

                            }
                        }

                    }

                }
        );

        // start receiving
        thread.start();

        // set the clientReceiveThread so it can be closed on disconnect.
        clientReceiveThread = thread;

    }





    /**
     *
     * @param prefix the message prefix
     * @param message the message
     * @return false if messageSending failed
     */
    public static boolean ClientSendString(String prefix, String message) {

        if ( client != null ) {

            try {

                // create new dataOutputStream to send string
                DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());

                // write prefix and bytes to the outputStream
                outputStream.write((prefix+"/prefend/"+message).getBytes(StandardCharsets.UTF_8));

                // send the message
                outputStream.flush();

                return true;

            } catch ( IOException ioException ) {

                // close the client
                CloseClient();

                return false;

            }

        } else {

            return false;

        }

    }

    /**
     *
     * @param prefix the prefix of the message
     * @param info some info
     * @param bytes the bytes to send
     * @return false if sending failed
     */
    public static boolean ClientSendBytes(String prefix, String info, byte[] bytes) {

        if ( client != null ) {

            try {

                // create new outputStream to write bytes on
                DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());

                // create new byte[] to send to the server
                byte[] sendBytes = new byte[bytes.length+prefix.length()+info.length()+7+9+9];

                String[] myFields = new String[]{"/bytes/", prefix, "/prefend/", info, "/infoend/"};  // put your fields in an array or list

                int bufferPosition = 0;

                // loop through each field and copy the data
                for (String field : myFields) {

                    byte[] stringBytes = field.getBytes();  // get bytes from string

                    System.arraycopy(stringBytes, 0, sendBytes, bufferPosition, stringBytes.length);  // copy src to dest

                    bufferPosition += stringBytes.length;  // advance index

                }

                // write to the outputStream
                outputStream.write(sendBytes);

                // send the bytes
                outputStream.flush();

                return true;

            } catch ( IOException ioException ) {

                CloseClient();

                return false;

            }

        } else {

            return false;

        }

    }




    /**
     * @param stringMessageHandler the StringMessageHandler to add
     * @param prefix the prefix were the handler should be called
     * @return false if the handler couldn't be registered because it is already a handler for that prefix registered
     */
    public static boolean registerStringMessageHandler(String prefix, StringMessageHandler stringMessageHandler) {

        // check if prefix is already used
        if( !stringMessageHandlers.containsKey( prefix ) ) {

            // add the StringMessageHandler to the handler list
            stringMessageHandlers.put(prefix, stringMessageHandler);

            // handler registered
            return true;
        }

        // handler couldn't be registered (prefix already used)
        return false;

    }

    /**
     * @param prefix the prefix were the handler should be called
     * @param byteMessageHandler the ByteMessageHandler to add
     * @return false if the handler couldn't be registered because it is already a handler fot that prefix registered
     */
    public static boolean registerByteMessageHandler(String prefix, ByteMessageHandler byteMessageHandler) {

        // check if prefix is already used
        if( !byteMessageHandlers.containsKey( prefix ) ) {

            // add the StringMessageHandler to the handler list
            byteMessageHandlers.put(prefix, byteMessageHandler);

            // handler registered
            return true;
        }

        // handler couldn't be registered (prefix already used)
        return false;
    }





    // close all connections (server, client)
    public static void CloseAll() {

        // disconnect and close the client
        CloseClient();

        // stop and close the server
        CloseServer();

    }

    // stop and close the server
    public static void CloseServer() {

        //check if a server exists
        if(server != null) {

            try {

                // try to close the server
                server.close();

            } catch (IOException ioException) { }

            // set the server to null
            server = null;

        }

    }

    // disconnect and close the client
    public static void CloseClient() {

        // check if client exists
        if(client != null) {

            try {

                // set receiving to false
                clientIsReceiving = false;

                // try to close the receiveThread of the client
                clientReceiveThread.interrupt();

            } catch ( Exception exception ) { }

            try {

                // try to close the client
                client.close();

            } catch (IOException ioException) { }

            // set the client to null
            client = null;

        }

    }
}
