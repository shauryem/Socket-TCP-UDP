// Approach: https://www.codejava.net/java-se/networking/java-udp-client-server-program-example
// Also: https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html

// Code inspiration are same as ones used for parallel client methods

import java.util.*;
import java.net.*;
import java.io.*;


class server_java_udp{
   // Declares scanner and udp socket references
    private DatagramSocket udpSocket;
    private static Scanner scanner;
   

    // Declares IP and Port for client connection
    private InetAddress clientAddress;
    private Integer clientPort;

    //ACK helpers
    private static final String ACK = "ACK";
    private static final byte [] bufACK = ACK.getBytes();


    public static void main(String[] args) {
    
  
        scanner = new Scanner(System.in);
        String IPAddress = "";
    
        // Gets current IP 
        try{
            InetAddress ipAddress = InetAddress.getLocalHost(); 
            IPAddress = ipAddress.getHostAddress();
        }catch(UnknownHostException err){
            System.out.println("Could not get server IP Address.");
            System.exit(0);
        }
    
        System.out.print("This machine's IP: ");
        System.out.println(IPAddress);
    
        System.out.println("Enter port:");
    
        //Waits for server port to listen to
        Integer userInput = Integer.valueOf(scanner.nextLine());
        System.out.println("Waiting for Connection");
        
        // Accepts client socket if input is valid
        if((userInput > 0) && (userInput < 65535)){
            server_java_udp server = new server_java_udp();
            server.ServerController(userInput);
        }
        else{
            System.out.println("Invalid port number.");
        }
    }


    public void ServerController(Integer port){
        
        // Server listens to given port until program is exited
        while(true){
            try{
                Boolean RecievePrepped = false;
                Boolean check1 = false;
                Boolean check0 = false;

                // new datagram socket with inputted port 
                this.udpSocket = new DatagramSocket(port);

                // Gets length of incoming message from client 
                String MessageLength = LengthInbound();

                // socket times out after 500 millis
                this.udpSocket.setSoTimeout(500);

                //gets length from string format 
                Integer startingLength = lengthDecode(MessageLength);
                
                if(!startingLength.equals(0)){
                    RecievePrepped = true;
                    ACKoutbound();
                }
                else{
                    RecievePrepped = false;
                }

                if(RecievePrepped){
                    
                    // Revieves command from client 
                    String incomingCommand = CommandInbound(startingLength);

                    // Sends ack back to client 
                    ACKoutbound();
                    
                    // Parses the cat command: splits the command itself and the filename after '>'
                    ArrayList<String> commandParse = new ArrayList<String>();
                    String filename = "server.txt";
                    String[] commandSplit = incomingCommand.split(">");
                    for(String c : commandSplit){
                        String temp = c.trim();
                        commandParse.add(temp);
                    }
                   
                    String command = commandParse.get(0);
                    if(commandParse.size() == 2)
                       filename = commandParse.get(1);

                    // Gets output of bash running comand on server
                    String commandResult = MessageDecode(command);
                  
                    // Makes new file with given file name or default and stores output of command
                    BufferedWriter toFile = new BufferedWriter(new FileWriter(filename, true));
                    toFile.write(commandResult);
                    toFile.close();
                    System.out.println("File " + filename + " saved.");

                    // Reads the info in the file and sends it back to the client
                    commandResult = MessageDecode("cat " + filename);
                    Integer outputLength = commandResult.length()*2;
                    int FAIL0 = 0;

                    // Tries to send length of output back to client up to 3 times
                    while(FAIL0 < 3 && !check0){
                        if(LengthOutbound(commandResult)){
                            FAIL0++;
                        }
                        if(ACKinbound()){
                            check0 = true;
                        }
                    }

                    //Calculates the number of sends needed to send back output to client
                    Integer numberOfSends = ((outputLength/512)+1);
                    for(int i=0; i<numberOfSends; i++){
                        int FAIL1 = 0;
                        
                        // Tries to send actual message of output back to client up to 3 times
                        while(FAIL1 < 3 && !check1 && check0){
                            String temp;
                            if(numberOfSends > 1){
                                temp = commandResult.substring(512*i, 512*(i+1));
                            }
                            else{
                                temp = commandResult;
                            }
                            if(CommandOutbound(temp)){
                                FAIL1++;
                            }
                            if(ACKinbound()){
                                check1 = true;
                            }
                        }
                    }
                }
                else{
                    if(!check0 || !check1){
                        System.out.println("Command not received, listening for command again.");
                    }
                }
            }catch(Exception e){
                System.out.println("Network communication error. Please try again.");
            }
            this.udpSocket.close();
        }
    }

    private String LengthInbound() throws SocketTimeoutException {
       
        // Recieves the length of following message from client
        byte [] Length = new byte [512];
        DatagramPacket incomingPacket = new DatagramPacket(Length, Length.length);
        try{
            this.udpSocket.receive(incomingPacket);
        }catch(IOException e){
            e.printStackTrace();
        }
        String LengthMessage = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
        this.clientAddress = incomingPacket.getAddress();
        this.clientPort = incomingPacket.getPort();
        return LengthMessage;
    }

    private Boolean LengthOutbound(String message){
       
       // Sends length of a message to the client in string format
        try{
            byte[] Result = message.getBytes();
            String lengthString = "length = ";
            String Length = lengthString.concat(Integer.toString(Result.length));
            byte[] LengthBuf = Length.getBytes();

            DatagramPacket clientPacket = new DatagramPacket(LengthBuf, LengthBuf.length, this.clientAddress, this.clientPort);
            this.udpSocket.send(clientPacket);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Boolean ACKoutbound(){
        
        // Sends confirmatioon ACK to client
        try{
            DatagramPacket ack= new DatagramPacket(bufACK, bufACK.length, this.clientAddress, this.clientPort);
            this.udpSocket.send(ack);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    private Boolean CommandOutbound(String message){
       
        // Sends a packet with the actual output of command back to client
        try{
            byte[] Command = message.getBytes();
            DatagramPacket outPacket = new DatagramPacket(Command, Command.length, this.clientAddress, this.clientPort);
            this.udpSocket.send(outPacket);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    private Integer lengthDecode(String lengthMessage){
       
        // Gets the the actual integer length of either input/output from string format
        Integer Length = 0;
        String [] parseCommand = lengthMessage.split(" ");
        ArrayList<String> parsedList = new ArrayList<String>();
        for(String c : parseCommand){
            parsedList.add(c);
        }
        if(parsedList.size() == 3){
            if(parsedList.get(0).equals("length") && parsedList.get(1).equals("=")){
                Length = Integer.valueOf(parsedList.get(2));
            }
        }
        return Length;
    }

    private Boolean ACKinbound(){
       
       // checks if input is equal to "ACK"
        try{
            String isACK = CommandInbound(bufACK.length);
            if(isACK.equals(ACK)){
                return true;
            }
            else{
                return false;
            }
        }catch(Exception e){
            return false;
        }
    }

    private String CommandInbound(Integer expectedLength){
        
        // Recieves packet from client containing command to execute on server
        String received;
        byte[] Buf = new byte[expectedLength];
        DatagramPacket receivedPacket = new DatagramPacket(Buf, Buf.length);
        try{
            this.udpSocket.receive(receivedPacket);
        }catch(IOException e){
            e.printStackTrace();
        }
        received = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
        return received;
    }

    private String MessageDecode(String message){
        
        // Processes command sent from client and returns the output of that command in a string
        String processedOutput = "";
        
        try{
            Process terminalCommand = Runtime.getRuntime().exec(message);
            InputStreamReader fromTerminal = new InputStreamReader(terminalCommand.getInputStream());
            BufferedReader chainFromTerminal = new BufferedReader(fromTerminal);
            for(String bufferString = chainFromTerminal.readLine(); bufferString != null; bufferString = chainFromTerminal.readLine()){
                processedOutput = processedOutput.concat(bufferString);
                processedOutput = processedOutput.concat("\n");
            }
            chainFromTerminal.close();
    
            terminalCommand.waitFor();
            terminalCommand.destroy();
        }catch(Exception e){
            return "Could not fetch file.";
        }
        return processedOutput;
    }



}