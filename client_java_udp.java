// Approach: https://www.codejava.net/java-se/networking/java-udp-client-server-program-example
// Also: https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html

import java.util.*;
import java.net.*;
import java.io.*;


class client_java_udp{
    // Declares scanner and udp socket references
    private static Scanner scanner;
    private DatagramSocket udpSocket;
   
    // Declares IP and Port for server connection
    private InetAddress serverIPAddress;
    private Integer port;

    //ACK helpers
    private static final String ACK = "ACK";
    private static final byte [] bufACK = ACK.getBytes();

    public static void main(String[] args) {
       
    
        System.out.print("Enter server name or IP address: ");

        //Takes in IP from user 
        scanner = new Scanner(System.in);
        String IPAddress = scanner.nextLine();
        ArrayList<String> ipParse = new ArrayList<String>();

        String[] ipSplit = IPAddress.split("\\.");
        for(String c : ipSplit){
            ipParse.add(c);
        }

        //Makes sure IP is valid
        for(int i=0; i<ipParse.size(); i++){
            String temp = ipParse.get(i);
            Integer tempInt = Integer.valueOf(temp);
            if(tempInt.intValue() > 255 || tempInt.intValue() < 0){
                System.out.println("Could not connect to server");
                System.exit(0);
            }
        }

        System.out.print("Enter server port number:");

        //Takes in port number
        Integer port = Integer.valueOf(scanner.nextLine());

        //Checks port validity
        if(port < 0 || port > 65535){
            System.out.println("Invalid port number");
            System.exit(0);
        }

        System.out.print("Enter command: ");
        
        //Takes in command from user
        String command = scanner.nextLine();
        client_java_udp client = new client_java_udp();

        //Connects to server 
        try{
            InetAddress ip = InetAddress.getByName(IPAddress);
            client.setUpServer(ip, port, command, "client.txt");
        }catch(Exception e){
            System.out.println("Failed to connect to server. Terminating.");
            System.exit(0);
        }
        
    }

    public void setUpServer(InetAddress IPAddress, Integer portInput, String command, String fileName){
       
        try{
            // Initializing instance variables 
            this.udpSocket = new DatagramSocket();
            this.port = portInput;
            this.serverIPAddress = IPAddress;

            // Success checks
            Boolean check0 = false;
            Boolean check1 = false;
            // fail checks 
            int Ncheck0 = 0;
            int Ncheck1 = 0;

            // Socket times out in 500 millis
            this.udpSocket.setSoTimeout(500);

            // Attempts to send command 3 times 
            while(Ncheck0 <3 && !check0){
                
                // Attempts to send length of command to server
                if(LengthOutbound(command)){
                    Ncheck0 ++;
                }
                else{
                    System.out.println("Could not send packet. Please make sure IP address and port number are correct and try again.");
                    System.exit(0);
                }
                if(ACKinbound()){
                    check0 = true;
                }
            }
            while(Ncheck1 <3 && !check1 && check0){
               
                // Attempts to send actual command to server 
                if(commandOutbound(command)){
                    Ncheck1++;
                }
                else{
                    System.out.println("Could not send packet. Please make sure IP address and port number are correct and try again.");
                    System.exit(0);
                }
                // Server recieved command
                if(ACKinbound()){
                    check1 = true;
                }

            }

            //Recieves output from server 
            if(check1 && check0){
                
                //Recieves length of incoming output
                String commandLength = LengthInbound();
                commandLength = Integer.toString(getLength(commandLength));

                // Sends ack of output length 
                ACKoutbound();
                String finalOutput = "";
                Integer outputLength = Integer.valueOf(commandLength);
                Integer numberOfSends = ((outputLength/512)+1);
                for(int i=0; i<numberOfSends; i++){
                    Integer temp;
                    if(numberOfSends > 1){
                        temp = 512;
                    }
                    else{
                        temp = outputLength;
                    }
                    finalOutput = finalOutput.concat(CommandInbound(temp));
                    ACKoutbound();
                }
                
                // Declares client side txt file and stores output from server
                BufferedWriter toFile = new BufferedWriter(new FileWriter("client.txt", true));
                toFile.write(finalOutput);
                toFile.close();
            }
            else{
                System.out.println("Network communication error.");
                System.exit(0);
            }
        }catch(Exception e){
            System.out.println("Network communication error.");
            System.exit(0);
        }
        this.udpSocket.close();
    }

    private Integer getLength(String message){
       
        // Gets length of inputted string from string format
        Integer Length = 0;
        String [] parseCmd = message.split(" ");
        ArrayList<String> parsedArgs = new ArrayList<String>();
        for(String c : parseCmd){
            parsedArgs.add(c);
        }
        
        if(parsedArgs.get(0).equals("length") && parsedArgs.get(1).equals("=")){
            Length = Integer.valueOf(parsedArgs.get(2));
        }
        
        return Length;
    }

    private Boolean commandOutbound(String message){
        // Inspiration: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets

        // Attempts to send a datagram packet to server w given command 
        try{
            byte[] bufferCommand = message.getBytes();
            DatagramPacket outgoingCommandPacket = new DatagramPacket(bufferCommand, bufferCommand.length, this.serverIPAddress, this.port);
            this.udpSocket.send(outgoingCommandPacket);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Boolean ACKoutbound(){
       // Inspiration: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets

       // Sends ACK to server 
        try{
            byte[] ackbuf = ACK.getBytes();
            DatagramPacket ackOutgoing = new DatagramPacket(ackbuf, ackbuf.length, this.serverIPAddress, this.port);
            this.udpSocket.send(ackOutgoing);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String LengthInbound(){
        // Inspiration: https://www.baeldung.com/udp-in-java

        // Takes in the incoming length of message
        byte [] bufferLength = new byte [512];
        DatagramPacket incoming = new DatagramPacket(bufferLength, bufferLength.length);
        try{
            this.udpSocket.receive(incoming);
        }catch(Exception e){
            e.printStackTrace();
        }
        String incomingMessage = new String(incoming.getData(), 0, incoming.getLength());
        return incomingMessage;
    }

    private Boolean ACKinbound(){

        // Checks if ACK sent is equal to the string "ACK"
        String isACK = CommandInbound(bufACK.length);
        if(isACK.equals(ACK)){
            return true;
        }
        else{
            return false;
        }

    }

    private Boolean LengthOutbound(String message){
        
       // Inspiration: https://stackoverflow.com/questions/8562689/need-to-send-a-udp-packet-and-receive-a-response-in-java
       
       // Sends length of command in String format to server 
       
        try{
            byte[] Result = message.getBytes();
            String introString = "length = ";
            String Length = introString.concat(Integer.toString(Result.length));
            byte[] bufLength = Length.getBytes();

            DatagramPacket resultLengthToClient = new DatagramPacket(bufLength, bufLength.length, this.serverIPAddress, this.port);
            this.udpSocket.send(resultLengthToClient);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    private String CommandInbound(Integer expectedLength){
       // Inspiration: https://stackoverflow.com/questions/8562689/need-to-send-a-udp-packet-and-receive-a-response-in-java
       // Takes in packet from server that contains outpur of command
        String receivedString;
        try{
            byte[] receive = new byte[expectedLength];
            DatagramPacket Packet = new DatagramPacket(receive, receive.length);
            this.udpSocket.receive(Packet);
            receivedString = new String(Packet.getData(), 0, Packet.getLength());
        }catch(Exception e){
            System.out.println("Connection timed out.");
            receivedString = ""; 
        }
        return receivedString;
    }


    
}
