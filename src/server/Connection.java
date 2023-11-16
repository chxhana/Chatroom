/**
 * This is the separate thread that services each
 * incoming echo client request.
 *
 * @author Greg Gagne 
 */

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class Connection implements Runnable
{
	private Socket	client;
	private Handler handler = new Handler();
	
	public Connection(Socket clientSocket) {
        this.client = clientSocket;
	}

    /**
     * This method runs in a separate thread.
     */	
	public void run() { 
		try {
			handler.process(client);
		}
		catch (java.io.IOException ioe) {
			System.err.println(ioe);
		}
	}
}

