import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Arthur on 4/21/18.
 */
public class server_java_tcp {

    public static void main(String args[]) throws IOException {
        // Following code (until 'END') taken from course lecture slides
        ServerSocket socket = new ServerSocket(6666);
        String input;

        boolean keepRunning = true;

        while (keepRunning) {
            // get command
            Socket connection = socket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            input = in.readLine();

            // get file name
            String[] pieces = input.split(">");
            String filename = null;
            if (pieces.length > 1) {
                filename = pieces[pieces.length - 1].trim();
            }

            if (filename == null) {
                throw new IOException(); // just in case
            }

            // reconstruct original command
            String command = pieces[0];
            for (int i=1; i<pieces.length - 2; i++) {
                command = command + ">" + pieces[i]; // original command COULD contain >'s
            }

            // run command
            Runtime rt = Runtime.getRuntime(); // copied from https://stackoverflow.com/a/8496537
            Process p = rt.exec(command); // run and send output to file
            BufferedReader commandOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            // write file
            File f = new File(filename);
            FileWriter writer = new FileWriter(f);

            String line = commandOutput.readLine();
            while (line != null) {
                writer.write(line+"\n");
                System.out.println(line);
                line = commandOutput.readLine();
            }
            writer.close();

            // send file size
            out.writeBytes(f.length() + "\n");

            // send file -- code adapted from https://gist.github.com/CarlEkerot/2693246
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[4096];
            while (fis.read(buffer) > 0) {
                out.write(buffer);
            }
            fis.close();

            System.out.println("Received input: "+input);
        }
        // END
    }
}