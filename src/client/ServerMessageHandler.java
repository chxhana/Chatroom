import java.io.*;
// I got this idea from stack overflow: https://stackoverflow.com/questions/22301892/print-to-console-while-waiting-for-input-in-java

class RunnableMessageHandler implements Runnable {
    private BufferedReader fromServer = null;

    public RunnableMessageHandler(BufferedReader fromServer) {
        this.fromServer = fromServer;
    }

    @Override
    public void run() {
        String serverOutput;
        String BRIGHT_CYAN = "\u001B[96m";
        String BOLD = "\u001B[1m";
        String RESET = "\u001B[0m";
        try {
            while (true) {
                serverOutput = fromServer.readLine();

                if (serverOutput == null)
                    break;

                // This is deleting the "> " before the user's input
                System.out.print("\b\b");

                if (serverOutput.length() == 1)
                    // This means the server sent back a code
                    Client.handleServerCode(Integer.parseInt(serverOutput));
                else
                    System.out.println(Client.parseServerOutput(serverOutput));

                // This is putting the "> " back
                System.out.print(BRIGHT_CYAN + BOLD + "> " + RESET);
           }
        }
        catch (IOException e) {
            return;
        }
    }
}
