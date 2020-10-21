import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.*;


class client_java_tcp{
    private String serverIPAddress;
    private Integer serverPortNumber;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private Socket tcpSocket;
    private static Scanner scanner;

    public void connect(String IPAddress, Integer port, String command, String fileName){
        String fromServer = "";
        this.serverPortNumber = port;
        this.serverIPAddress = IPAddress;
        try{
            this.tcpSocket = new Socket(this.serverIPAddress, this.serverPortNumber);
            //System.out.println("Socket created successfully");
        }catch(Exception e){
            //e.printStackTrace();
            System.out.println("Could not connect to server");
        }

        try{
            //System.out.println("Setting up streams");
            this.outStream = new ObjectOutputStream(this.tcpSocket.getOutputStream());
            this.inStream = new ObjectInputStream(this.tcpSocket.getInputStream());

            //System.out.print("Streams set up, about to write command:");
            //System.out.println(command);
            this.outStream.writeObject(command);
            //System.out.println("Wrote command to server");
            fromServer = (String) this.inStream.readObject();

            //System.out.print("Message received: ");
            //System.out.println(fromServer);

            //writes output to file instead of standard input.
            BufferedWriter toFile = new BufferedWriter(new FileWriter(fileName, true));
            toFile.write(fromServer);
            toFile.close();

        }catch(Exception e){
            //e.printStackTrace();
            System.out.println("Error communicating with server");
        }
    }


    public static void main(String args[]){      
        String IPAddress = "";
        Integer port;
        String command;
        String fileName = "client.txt";
        String COULD_NOT_CONNECT = "Could not connect to server.";
        String INVALID_PORT = "Invalid port number";
        String FAIL_SEND = "Failed to send command. Terminating.";
        String CANNOT_FETCH = "Could not fetch file";
        
        System.out.print("Enter server name or IP address: ");
        scanner = new Scanner(System.in);
        IPAddress = scanner.nextLine();
        ArrayList<String> ipParse = new ArrayList<String>();

        String[] ipSplit = IPAddress.split("\\.");
        for(String c : ipSplit){
            ipParse.add(c);
        }

        for(int i=0; i<ipParse.size(); i++){
            String temp = ipParse.get(i);
            Integer tempInt = Integer.valueOf(temp);
            if(tempInt.intValue() > 255 || tempInt.intValue() < 0){
                System.out.println(COULD_NOT_CONNECT);
                System.exit(0);
            }
        }

        System.out.print("Enter server port number:");
        port = Integer.valueOf(scanner.nextLine());
        if(port < 0 || port > 65535){
            System.out.println(INVALID_PORT);
            System.exit(0);
        }

        System.out.print("Enter command: ");
        command = scanner.nextLine();
        client_java_tcp client = new client_java_tcp();
        
        // ArrayList<String> commandParse = new ArrayList<String>();

        // String[] commandSplit = command.split(">");
        // for(String c : commandSplit){
        //     String temp = c.trim();
        //     commandParse.add(temp);
        // }
        // // if(commandParse.size() != 2){
        // //     System.out.println("Invalid command was entered");
        // //     System.exit(0);
        // // }
        // command = commandParse.get(0);
        // if(commandParse.size() == 2)
        //     fileName = commandParse.get(1);
        client.connect(IPAddress, port, command, fileName);  

    }
}