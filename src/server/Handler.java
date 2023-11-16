import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Handler {
    private Socket clientSocket = null;
    private BufferedReader fromClient = null;
    private PrintWriter toClient = null;
    private String username = "";
    private static ConcurrentHashMap<String, PrintWriter> userMap = new ConcurrentHashMap<String, PrintWriter>();

    public void process(Socket clientSocket) throws IOException {
        try  {
            this.clientSocket = clientSocket;
            fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            toClient = new PrintWriter(clientSocket.getOutputStream(), true);
            String clientCommand;

            while (true) {
                clientCommand = fromClient.readLine();

                // Likely means that the user left without an exit command
                if (clientCommand == null)
                    break;

                int returnCode = processCommand(clientCommand);

                // The user left
                if (returnCode == -1) {
                    break;
                }
            }
        } catch (IOException e) {
            //System.out.println(e);
            e.printStackTrace();
        } finally {
            // Redundancy in case the user ctrl+c
            if (userMap.keySet().contains(this.username)) {
                userMap.remove(this.username);
                for (PrintWriter userWriter : userMap.values()) {
                    String formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                    userWriter.println("broadcast<server," + formattedTime + "," + this.username + " left the server");
                }
            }

            if (clientSocket != null)
                clientSocket.close();
            if (fromClient != null)
                fromClient.close();
            if (toClient != null)
                toClient.close();
        }
    }

    public int processCommand(String command) throws IOException {
        String commandHeader = command.split("<")[0];

        if (commandHeader.equals("user"))
            return processNewUserRequest(command);

        if (commandHeader.equals("exit"))
            return processExitRequest(command);

        if (commandHeader.equals("ls"))
            return processUserlistRequest(command);

        if (commandHeader.equals("broadcast"))
            return processBroadcastRequest(command);

        if (commandHeader.equals("private"))
            return processPrivateRequest(command);

        return 1;
    }

    public int processNewUserRequest(String command) {
        // Means that we have an empty command body
        if (command.equals("user<>")) {
            toClient.println(3);
            return 1;
        }

        String commandBody = command.substring(command.indexOf("<")+1,command.length()-1);

        // Username taken
        if (userMap.keySet().contains(commandBody) || commandBody.equals("server")) {
            toClient.println(1);
            return 1;
        }
        
        // Reserved character used
        if (commandBody.contains("<") || commandBody.contains(">") || commandBody.contains(",")) {
            toClient.println(2);
            return 1;
        }

        // Length of username is invalid (checking for < 1 is redundant)
        if (commandBody.length() < 1 || commandBody.length() > 20) {
            toClient.println(3);
            return 1;
        }

        // Success
        userMap.put(commandBody, toClient);
        this.username = commandBody;

        // We'll need to make this a broadcast message at some point
        userMap.get(commandBody).println(4);
        for (PrintWriter userWriter : userMap.values()) {
            String formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            userWriter.println("broadcast<server," + formattedTime + "," + commandBody + " joined the server");
        }

        return 1;
    }

    public int processExitRequest(String command) {
        String[] commandParts = command.split("<");
        String commandBody = commandParts[1].split(">")[0];

        userMap.remove(this.username);
        for (PrintWriter userWriter : userMap.values()) {
            String formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            userWriter.println("broadcast<server," + formattedTime + "," + this.username + " left the server");
        }

        try {
            toClient.close();
            fromClient.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        return -1;
    }

    public int processUserlistRequest(String command) {
        String[] commandParts = command.split("<");
        String username = commandParts[1].split(">")[0];
        
        String userlist = "";
        for (String user : userMap.keySet())
            userlist += ("," + user);

        userlist = "userlist<" + userlist.substring(1) + ">";
        userMap.get(username).println(userlist);
        return 1;
    }

    public int processBroadcastRequest(String command) {
        String commandString = command.substring(command.indexOf("<")+1, command.length()-1);
        String[] commandParts = commandString.split(",");
        String senderUsername = commandParts[0];
        String time = commandParts[1];
        String message = commandString.substring(commandString.indexOf(time) + time.length()+1);

        // Check for invalid message length (0 or more than 1000)
        if (message.length() == 0 || message.length() > 1000) {
            userMap.get(senderUsername).println(5);
            return 1;
        }

        // Check if a reserved character is used (angle brackets)
        if (message.contains("<") || message.contains(">")) {
            userMap.get(senderUsername).println(6);
            return 1;
        }

        for (PrintWriter userWriter : userMap.values())
            userWriter.println(command);

        userMap.get(senderUsername).println(8);
        return 1;
    }

    public int processPrivateRequest(String command) {
        String commandString = command.substring(command.indexOf("<")+1, command.length()-1);
        String[] commandParts = commandString.split(",");
        String senderUsername = commandParts[0];
        String recipientUsername = commandParts[1];
        String time = commandParts[2];
        String message = commandString.substring(commandString.indexOf(time) + time.length()+1);

        // Check for invalid message length (0 or more than 1000)
        if (message.length() == 0 || message.length() > 1000) {
            userMap.get(senderUsername).println(5);
            return 1;
        }

        // Check if a reserved character is used (angle brackets)
        if (message.contains("<") || message.contains(">")) {
            userMap.get(senderUsername).println(6);
            return 1;
        }

        // Check that the recipient exists
        if (!userMap.keySet().contains(recipientUsername)) {
            userMap.get(senderUsername).println(9);
            return 1;
        }

        userMap.get(recipientUsername).println(command);
        userMap.get(senderUsername).println(command);
        userMap.get(senderUsername).println(7);
        return 1;
    }
}
