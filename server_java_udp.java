import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.*;

class server_java_udp{
    private DatagramSocket udpSocket;
    private Integer clientPort;
    private InetAddress clientAddress;
    private static Scanner scanner;
    private static final String ACK = "ACK";
    private static final byte [] bufACK = ACK.getBytes();

    public void setupUDPServer(Integer port){
        
        while(true){
            try{
                Boolean readyForData = false;
                Boolean SUCCEED1 = false;
                Boolean SUCCEED0 = false;
                this.udpSocket = new DatagramSocket(port);

                String incomingLengthMessage = receiveLength();
                this.udpSocket.setSoTimeout(500);
                Integer initialLength = parseLength(incomingLengthMessage);
                
                if(!initialLength.equals(0)){
                    readyForData = true;
                    sendACK();
                }
                else{
                    readyForData = false;
                }

                if(readyForData){
                    //System.out.println("ACK sent, ready for command");
                    String incomingCommand = receiveCommand(initialLength);
                    sendACK();
                    //System.out.print("Command from client: ");
                    //System.out.println(incomingCommand);
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
                    String commandResult = processMessage(command);
                    //System.out.println(commandResult);
                    BufferedWriter toFile = new BufferedWriter(new FileWriter(filename, true));
                    toFile.write(commandResult);
                    toFile.close();
                    System.out.println("File " + filename + " saved.");
                    commandResult = processMessage("cat " + filename);
                    Integer outputLength = commandResult.length()*2;
                    int FAIL0 = 0;
                    while(FAIL0 < 3 && !SUCCEED0){
                        if(sendLength(commandResult)){
                            //System.out.println("Success sending result length");
                            FAIL0++;
                        }
                        else{
                            //System.out.println("Error");
                        }
                        if(receiveACK()){
                            SUCCEED0 = true;
                            //System.out.println("ReceivedACK!");
                        
                        }
                        else{
                            //System.out.println("Did not receive ACK");
                        }
                    }
                    Integer numberOfSends = ((outputLength/512)+1);
                    //System.out.println("number of sends:");
                    //System.out.println(numberOfSends);
                    for(int i=0; i<numberOfSends; i++){
                        int FAIL1 = 0;
                        while(FAIL1 < 3 && !SUCCEED1 && SUCCEED0){
                            String temp;
                            if(numberOfSends > 1){
                                temp = commandResult.substring(512*i, 512*(i+1));
                            }
                            else{
                                temp = commandResult;
                            }
                            if(sendCommand(temp)){
                                FAIL1++;
                                //System.out.println("Sent command results to client");
                            }
                            else{
                                //System.out.println("Failed to send command");
                            }
                            if(receiveACK()){
                                SUCCEED1 = true;
                                //System.out.println("Client received command results");
                            }
                            else{
                                //System.out.println("Client did not receive command results");
                            }
                        }
                    }
                }
                else{
                    if(!SUCCEED0 || !SUCCEED1){
                        System.out.println("Command not received, listening for command again.");
                    }
                }
            }catch(Exception e){
                //e.printStackTrace();
                System.out.println("Network communication error. Please try again.");
            }
            this.udpSocket.close();
        }
    }

    private String receiveLength() throws SocketTimeoutException {
        byte [] bufferLength = new byte [512];
        DatagramPacket incoming = new DatagramPacket(bufferLength, bufferLength.length);
        try{
            this.udpSocket.receive(incoming);
        }catch(IOException e){
            e.printStackTrace();
        }
        String incomingLengthMessage = new String(incoming.getData(), 0, incoming.getLength());
        this.clientAddress = incoming.getAddress();
        this.clientPort = incoming.getPort();
        //System.out.println("Returning incomingLengthMessage");
        //System.out.println(incomingLengthMessage);
        return incomingLengthMessage;
    }

    private Boolean sendLength(String message){
        try{
            byte[] bufResult = message.getBytes();
            String lengthEquals = "length = ";
            String resultLength = lengthEquals.concat(Integer.toString(bufResult.length));
            byte[] resultLengthBuf = resultLength.getBytes();

            DatagramPacket resultLengthToClient = new DatagramPacket(resultLengthBuf, resultLengthBuf.length, this.clientAddress, this.clientPort);
            this.udpSocket.send(resultLengthToClient);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Boolean sendACK(){
        try{
            DatagramPacket ackOutgoing = new DatagramPacket(bufACK, bufACK.length, this.clientAddress, this.clientPort);
            this.udpSocket.send(ackOutgoing);
        }catch(Exception e){
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    private Boolean sendCommand(String message){
        try{
            byte[] bufferCommand = message.getBytes();
            DatagramPacket outgoingCommandPacket = new DatagramPacket(bufferCommand, bufferCommand.length, this.clientAddress, this.clientPort);
            this.udpSocket.send(outgoingCommandPacket);
        }catch(Exception e){
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    private Integer parseLength(String lengthMessage){
        Integer incomingLength = 0;
        String [] parseCommand = lengthMessage.split(" ");
        ArrayList<String> parsedArgsArrList = new ArrayList<String>();
        for(String c : parseCommand){
            parsedArgsArrList.add(c);
        }
        if(parsedArgsArrList.size() == 3){
            if(parsedArgsArrList.get(0).equals("length") && parsedArgsArrList.get(1).equals("=")){
                incomingLength = Integer.valueOf(parsedArgsArrList.get(2));
            }
        }
        return incomingLength;
    }

    private Boolean receiveACK(){
        try{
            String isACK = receiveCommand(bufACK.length);
            if(isACK.equals(ACK)){
                return true;
            }
            else{
                return false;
            }
        }catch(Exception e){
            //e.printStackTrace();
            return false;
        }
    }

    private String receiveCommand(Integer expectedLength){
        String receivedString;
        byte[] receiveBuf = new byte[expectedLength];
        DatagramPacket receivedPacket = new DatagramPacket(receiveBuf, receiveBuf.length);
        try{
            this.udpSocket.receive(receivedPacket);
        }catch(IOException e){
            e.printStackTrace();
        }
        receivedString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
        return receivedString;
    }

    private String processMessage(String message){
        String processedOutput = "";
        //try block credit goes to https://stackoverflow.com/questions/792024/how-to-execute-system-commands-linux-bsd-using-java
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


public static void main(String[] args) {
    scanner = new Scanner(System.in);
    String IPAddress = "";
    try{
        InetAddress ipAddress = InetAddress.getLocalHost(); 
        IPAddress = ipAddress.getHostAddress();
    }catch(UnknownHostException uhe){
        System.out.println("Could not get server IP Address.");
        System.exit(0);
    }
    System.out.print("This machine's IP: ");
    System.out.println(IPAddress);

    System.out.println("Enter port:");
    Integer userInput = Integer.valueOf(scanner.nextLine());

    if((userInput > 0) && (userInput < 65535)){
        server_java_udp server = new server_java_udp();
        server.setupUDPServer(userInput);
    }
    else{
        System.out.println("Invalid port number.");
    }
}
}