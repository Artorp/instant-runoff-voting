package no.artorp.instantrunoffvoting.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Queue;

import com.google.gson.Gson;

import no.artorp.instantrunoffvoting.json_models.ConnectionClosed;

public class ClientSocketHandler implements Runnable {

	private ClientLobbyController controller;
	private Socket socketToServer;

	private BufferedReader fromServer;
	private PrintStream toServer;
	private boolean disconnected = false;
	private final Queue<String> messageToServer = new ArrayDeque<>();
	private final Queue<String> messageFromServer = new ArrayDeque<>();

	public ClientSocketHandler(Socket serverSocket) {
		this.socketToServer = serverSocket;
	}

	public void setController(ClientLobbyController controller) {
		this.controller = controller;
	}

	@Override
	public void run() {
		// Open streams to server
		try {
			toServer = new PrintStream(socketToServer.getOutputStream(), true, "UTF-8");
			fromServer = new BufferedReader(
					new InputStreamReader(socketToServer.getInputStream(), Charset.forName("UTF-8")));
			while (!Thread.currentThread().isInterrupted()) {
				synchronized (this) {
					this.wait(100);
					// Read from socket
					String line;
					while (fromServer.ready() && (line = fromServer.readLine()) != null) {
						if (!messageFromServer.offer(line)) {
							System.err.println("Error: Received message from server but could not add it to the queue\n"
									+ "Message was:\n" + line);
						}
					}
					if (!messageFromServer.isEmpty()) {
						controller.gotMessageFromServer();
					}
					// Write to socket
					while ((line = messageToServer.poll()) != null) {
						toServer.println(line);
					}
				}
			}
		} catch (InterruptedException e) {
			System.out.println("Client thread was interrupted, exiting");
		} catch (IOException e) {
			if (!disconnected) {
				System.err.println("Error on socket to server");
				e.printStackTrace();
			}
		}
		if (!disconnected) {
			tellOtherWeClosed();
		}
		stopStreams();
	}
	
	private void tellOtherWeClosed() {
		ConnectionClosed cc = new ConnectionClosed();
		String json = new Gson().toJson(cc);
		toServer.println(json);
	}

	private void stopHelper(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
			}
		}
	}

	private void stopStreams() {
		stopHelper(fromServer);
		stopHelper(toServer);
		stopHelper(socketToServer);
	}

	public boolean sendMessageToServer(String message) {
		boolean didSend = false;
		synchronized (this) {
			didSend = messageToServer.offer(message);
			this.notifyAll();
		}
		return didSend;
	}

	public boolean hasMessage() {
		synchronized (this) {
			return messageFromServer.size() > 0;
		}
	}

	/**
	 * Returns a JSON message from the server
	 * 
	 * @return the head of the message queue, or {@code null} if no message
	 */
	public String getMessage() {
		synchronized (this) {
			return messageFromServer.poll();
		}
	}

	/**
	 * Close the socket without telling the other connection. Usually called if we
	 * received close connection message from them.
	 */
	public void disconnectSilently() {
		synchronized (this) {
			this.disconnected = true;
			stopStreams();
		}
	}

}
