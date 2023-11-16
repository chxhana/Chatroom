/**
 * An echo server listening on port 6007. 
 * This server reads from the client
 * and echoes back the result. 
 *
 * This services each request in a separate thread.
 *
 * This conforms to RFC 862 for echo servers.
 *
 * @author - Greg Gagne.
 */

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class  Server
{
	public static final int DEFAULT_PORT = 5040;
    
    // construct a thread pool for concurrency	
	private static final Executor exec = Executors.newCachedThreadPool();
	
	public static void main(String[] args) throws IOException {
		
        ServerSocket sock = null;
		try {
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);
			System.out.println("Server is listening on port " + DEFAULT_PORT);

			while (true) {
				Runnable task = new Connection(sock.accept());
				exec.execute(task);
			}
		}
		catch (IOException ioe) {
            System.err.println(ioe); 
        }
		finally {
            //for (BufferedReader clientReader : clients)
             //   clientReader.close();
		}
	}
}
