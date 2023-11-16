import java.io.*;
import java.net.*;
import java.util.Scanner; 
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Client {

    // Format:
    // For broadcasts: (broadcast)hello
    // For private messages: (private recipient_username)hhello
    // For listing the users: ls
    // For leaving: exit or ctrl+c

    public static final int SERVERPORT = 5040;

    public static void main(String[] args) {
        String userInput;
        String command;
        String username;

        if (args.length != 1) {
            System.out.println("You need to give an IP address");
            System.exit(0);
        }

        try (
            Socket clientSocket = new Socket(args[0], SERVERPORT);
            PrintWriter toServer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            Scanner scanner = new Scanner(System.in); 
        ) {
            // Setup username
            while (true) {
                System.out.print("Enter your username: ");
                userInput = scanner.nextLine();
                username = userInput;
                toServer.println("user<" + userInput + ">"); 
                String serverUsernameResponse = fromServer.readLine();
                int returnCode = handleUsernameServerCode(Integer.parseInt(serverUsernameResponse));

                if (returnCode == 1)
                    break;
            }

            new Thread(new RunnableMessageHandler(fromServer)).start();

            // Handle chatting phase
            while (true) {
                userInput = scanner.nextLine();
                command = parseUserInput(userInput, username);

                // Means that we have some invalid input
                if (command.equals("-1"))
                    continue;

                toServer.println(command);

                if (userInput.stripTrailing().equals("exit"))
                    break;
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
        finally {
            System.out.println("You've left the server");
        }
    }

    // Handle codes sent from server for username requests (1-4)
    public static int handleUsernameServerCode(int code) {
        switch (code) {
            case 1:
                System.out.println("Username taken");
                return -1;
            case 2:
                System.out.println("Protected character used");
                return -1;
            case 3:
                System.out.println("Length of the username is invalid");
                return -1;
            case 4:
                String BRIGHT_GREEN = "\u001B[92m";
                String BOLD = "\u001B[1m";
                String RESET = "\u001B[0m";
                System.out.println(BRIGHT_GREEN + BOLD + "Success" + RESET);
                return 1;
        }
        return -1;
    }
    
    // Handle a code sent from the server (5-9)
    public static void handleServerCode(int code) {
        String userInput;
        String command;

        switch (code) {
            case 5:
                System.out.println("Invalid message length");
                return;
            case 6:
                System.out.println("Reserved character used");
                return;
            case 7:
                return;
            case 8:
                return;
            case 9:
                System.out.println("User not found");
                return;
        }
    }

    public static String parseUserInput(String userInput, String username) {
        userInput = userInput.stripTrailing();

        if (userInput.length() == 0)
            return "-1";

        if (userInput.equals("exit"))
            return "exit<" + username + ">";
        if (userInput.equals("ls"))
            return "ls<" + username + ">";

        // Check if there aren't parenthesis
        if (userInput.charAt(0) != '(' || !userInput.contains(")")) {
            System.out.println("Invalid request, message must contain parenthesis");
            String BRIGHT_CYAN = "\u001B[96m";
            String BOLD = "\u001B[1m";
            String RESET = "\u001B[0m";
            System.out.print(BRIGHT_CYAN + BOLD + "> " + RESET);
            return "-1";
        }

        // User probably just has 2 parenthesis with nothing in them
        if (userInput.length() <= 2) {
            System.out.println("Invalid command");
            String BRIGHT_CYAN = "\u001B[96m";
            String BOLD = "\u001B[1m";
            String RESET = "\u001B[0m";
            System.out.print(BRIGHT_CYAN + BOLD + "> " + RESET);
            return "-1";
        }

        // Check that what's in parenthesis is 'broadcast' or 'private'
        // If it's private, check that there's room for a username with substring
        String betweenParenthesis = userInput.substring(1, userInput.indexOf(")"));
        if (!(betweenParenthesis.equals("broadcast") || (betweenParenthesis.substring(0, 7).equals("private") && betweenParenthesis.length() > 7))) {
            System.out.println("Invalid: '" + betweenParenthesis + "'");
            String BRIGHT_CYAN = "\u001B[96m";
            String BOLD = "\u001B[1m";
            String RESET = "\u001B[0m";
            System.out.print(BRIGHT_CYAN + BOLD + "> " + RESET);
            return "-1";
        }

        // Handle broadcast messages
        if (betweenParenthesis.equals("broadcast")) {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedTime = time.format(formatter);
            String message = userInput.split("\\)", 2)[1];

            String command = "broadcast<"+username+","+formattedTime+","+message+">";
            return command;
        }

        // Handle private messages
        if (betweenParenthesis.substring(0, 7).equals("private")) {
            String formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String recipient = betweenParenthesis.split(" ")[1];
            String message = userInput.split("\\)", 2)[1];

            String command = "private<"+username+","+recipient+","+formattedTime+","+message+">";
            return command;
        }

        return "-1";
    }

    public static String parseServerOutput(String serverOutput) {
        String[] outputParts = serverOutput.split("<");
        String header = outputParts[0];
        String body = outputParts[1].split(">")[0];
        String[] bodySegments = body.split(",");

        if (header.equals("userlist")) {
            String formattedString = "";
            for (String username : bodySegments)
                formattedString += "\n" + username;
            return formattedString.substring(1);
        }

        // Broadcasts
        if (header.equals("broadcast")) {
            String senderUsername = bodySegments[0];
            String time = bodySegments[1];
            String message = serverOutput.substring(serverOutput.indexOf(time)+time.length()+1,serverOutput.length()-1);
            return "(" + senderUsername + " " + time + ") " + message;
        }

        // Private Messages
        if (header.equals("private")) {
            String senderUsername = bodySegments[0];
            String recipientUsername = bodySegments[1];
            String time = bodySegments[2];
            String message = serverOutput.substring(serverOutput.indexOf(time)+time.length()+1,serverOutput.length()-1);
            return "([private] " + senderUsername +" " + time + ") " + message;
        }

        return "";
    }
}
