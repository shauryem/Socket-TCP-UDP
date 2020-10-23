import java.util.*;
import java.net.*;
import java.io.*;


class server_java_tcp{
    // Declaring scanner socket, and server socket references
    private static Scanner scanner;
    private ServerSocket tcpServerSocket;
    private Socket tcpSocket;

    // Declaring helping fields
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private Integer LPort;
    public static void main(String args[]){
        String IPAddress = "";

        // Gets and prints local IP Address
        try{
            InetAddress ipAddress = InetAddress.getLocalHost(); 
            IPAddress = ipAddress.getHostAddress();
        }catch(UnknownHostException err){
            err.printStackTrace();
        }

        System.out.print("Current IP: ");
        System.out.println(IPAddress);
        scanner = new Scanner(System.in);
        System.out.println("Enter port:");

        //Takes in port
        Integer userInput = Integer.valueOf(scanner.nextLine());

        // if port is valid, accept sockets 
        if((userInput > 0) && (userInput < 65535)){
            server_java_tcp server = new server_java_tcp();
            server.runServer(userInput);
        }
    }

    void runServer(Integer lp){
        this.LPort = lp;

        //Attempts to Create a ServerSocket listening on inputted port
        try{
            this.tcpServerSocket = new ServerSocket(this.LPort);
        }catch(Exception e){
            System.out.println("Could not create server socket. please try again.");
            System.exit(0);
        }
        while(true){
            try{
                // Attempts to accept incoming socket
                System.out.println("Waiting for Connection");
                this.tcpSocket = this.tcpServerSocket.accept();

                //Initializes input and output streams
                this.inStream = new ObjectInputStream(this.tcpSocket.getInputStream());
                this.outStream = new ObjectOutputStream(this.tcpSocket.getOutputStream());
                
                //Reads in command sent from client 
                String message = (String) this.inStream.readObject();

                // Splits command and file name
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
                  
                // Gets result of command in a string    
                String outputToClient = MessageDecoder(command);

                // Writes result to a file of either given file name or just server.txt
                BufferedWriter toFile = new BufferedWriter(new FileWriter(filename, true));
                toFile.write(outputToClient);
                toFile.close();
                System.out.println("File "+ filename + " saved.");

                // Reads the results back out of the saved file and sends it back to the client
                String resp = MessageDecoder("cat "+ filename);
                this.outStream.writeObject(resp);
            

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    
    }

    private String MessageDecoder(String message){
         //Inspiration: https://mkyong.com/java/how-to-execute-shell-command-from-java/
        String processedOutput = "";

        // Runs command as a process and saves the result in a string 
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