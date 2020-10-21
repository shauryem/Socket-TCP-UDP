  
import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.*;


class server_java_tcp{
private Integer ListeningPort;
private ServerSocket tcpServerSocket;
private Socket tcpSocket;
//private BufferedReader bufInStream;
private ObjectInputStream inStream;
private ObjectOutputStream outStream;
//private BufferedWriter bufOutStream;
private static Scanner scanner;

public void setListeningPort(Integer lp){
    this.ListeningPort = lp;
}

void setupTCPServer(){
    try{
        this.tcpServerSocket = new ServerSocket(this.ListeningPort);
    }catch(Exception e){
        //e.printStackTrace();
        System.out.println("Could not create server socket. please try again.");
        System.exit(0);
    }
    while(true){
        try{
            //create new server socket and listen
            System.out.println("Server waiting for request");
            this.tcpSocket = this.tcpServerSocket.accept();
            
            //System.out.println("SocketAccepted");

            //set up streams
            this.inStream = new ObjectInputStream(this.tcpSocket.getInputStream());
            this.outStream = new ObjectOutputStream(this.tcpSocket.getOutputStream());
            
            //System.out.println("Input streams created");

            //get terminal command from client
            String message = (String) this.inStream.readObject();

            //System.out.print("Message is: ");
            //System.out.println(message);

            //run command on server
            ArrayList<String> commandParse = new ArrayList<String>();
            String filename = "server.txt";
            String[] commandSplit = message.split(">");
            for(String c : commandSplit){
                String temp = c.trim();
                commandParse.add(temp);
            }
           
            String command = commandParse.get(0);
            if(commandParse.size() == 2)
               filename = commandParse.get(1);
                
            String outputToClient = processMessage(command);
            BufferedWriter toFile = new BufferedWriter(new FileWriter(filename, true));
            toFile.write(outputToClient);
            toFile.close();
            System.out.println("File "+ filename + " saved.");
            //System.out.println("Ran command, output is: ");
            //System.out.println(outputToClient);
            // System.out.println(command);
            // System.out.println(filename);
            // System.out.println(outputToClient);
            //send output back to client
            //System.out.println("Writing output back to client");
            String resp = processMessage("cat "+ filename);
            // System.out.println(resp);
            this.outStream.writeObject(resp);
           

        }catch(Exception e){
            e.printStackTrace();
        }
    }
 
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


public static void main(String args[]){
    String IPAddress = "";
    try{
        InetAddress ipAddress = InetAddress.getLocalHost(); 
        IPAddress = ipAddress.getHostAddress();
    }catch(UnknownHostException uhe){
        uhe.printStackTrace();
    }
    System.out.print("This machine's IP: ");
    System.out.println(IPAddress);

    scanner = new Scanner(System.in);
    System.out.println("Server Listening Port:");
    Integer userInput = Integer.valueOf(scanner.nextLine());

    if((userInput > 0) && (userInput < 65535)){
        server_java_tcp server = new server_java_tcp();
        server.setListeningPort(userInput);
        server.setupTCPServer();
    }
}

}