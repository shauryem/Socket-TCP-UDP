import java.util.*;
import java.net.*;
import java.io.*;



class client_java_tcp{
    // Class fields that make references to scanner and socket
    private static Scanner scanner;
    private Socket tcpSocket;
    
    // Other class fields declared
    private String serverIPAddress;
    private Integer serverPortNumber;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
   
    public static void main(String args[]){      
      
        
        System.out.print("Enter server name or IP address: ");

        //Takes in IP or 127.0.0.1 for local
        scanner = new Scanner(System.in);
        String IPAddress = scanner.nextLine();
        ArrayList<String> ipParse = new ArrayList<String>();

        String[] ipSplit = IPAddress.split("\\.");
        for(String c : ipSplit){
            ipParse.add(c);
        }

        for(int i=0; i<ipParse.size(); i++){
            String temp = ipParse.get(i);
            Integer tempInt = Integer.valueOf(temp);

            // if any connection compnents are above limits, invalid connection request
            if(tempInt.intValue() > 255 || tempInt.intValue() < 0){
                System.out.println("Could not connect to server");
                System.exit(0);
            }
        }

        System.out.print("Enter server port number:");

        //Takes in port number
        Integer port = Integer.valueOf(scanner.nextLine());

        //Error if invalid port
        if(port < 0 || port > 65535){
            System.out.println("Invalid port number");
            System.exit(0);
        }

        System.out.print("Enter command: ");

        //Takes in command 
        String command = scanner.nextLine();
        client_java_tcp client = new client_java_tcp();
        //Connects to server and sends command 
        client.handleServer(IPAddress, port, command, "client.txt");  

    }

    public void handleServer(String IPAddress, Integer port, String command, String fileName){
        // Initializing instance variables
        String fromServer = "";
        this.serverPortNumber = port;
        this.serverIPAddress = IPAddress;
        // Attempts to create a socket with given IP and port 
        try{
            this.tcpSocket = new Socket(this.serverIPAddress, this.serverPortNumber);
        }catch(Exception e){
            System.out.println("Could not connect to server");
        }

        try{
            // Initializes input and output streams of the socket instance
            this.outStream = new ObjectOutputStream(this.tcpSocket.getOutputStream());
            this.inStream = new ObjectInputStream(this.tcpSocket.getInputStream());

            // Writes the user inputted command to the server
            this.outStream.writeObject(command);
            // Reads the output back from the server
            fromServer = (String) this.inStream.readObject();

            //Writes server responce to text file client.txt
            BufferedWriter toFile = new BufferedWriter(new FileWriter(fileName, true));
            toFile.write(fromServer);
            toFile.close();

        }catch(Exception e){
            System.out.println("Error communicating with server");
        }
    }



}