package no.artorp.instantrunoffvoting.server;

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

import no.artorp.instantrunoffvoting.globals.Threads;
import no.artorp.instantrunoffvoting.json_models.ConnectionClosed;

/**
 * Communicates with a client through a socket.
 */
public class ClientThread implements Runnable {

	private HostLobbyController controller;
	private ObservableClient myObservableClient;
	private String name;
	private Socket clientSocket;
	private Queue<String> messageToClient = new ArrayDeque<>();
	private Queue<String> messageFromClient = new ArrayDeque<>();

	private PrintStream toClient;
	private BufferedReader fromClient;
	private boolean disconnectSilently = false;
	private String goodbyeMessage = null;

	public ClientThread(HostLobbyController controller, String name, Socket clientSocket) {
		this.controller = controller;
		this.name = name;
		this.clientSocket = clientSocket;
	}

	public void setObservableClient(ObservableClient client) {
		synchronized (this) {
			this.myObservableClient = client;
		}
	}

	public ObservableClient getObservableClient() {
		return this.myObservableClient;
	}

	@Override
	public void run() {
		try {
			this.toClient = new PrintStream(clientSocket.getOutputStream(), true, "UTF-8");
			this.fromClient = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream(), Charset.forName("UTF-8")));
			synchronized (this) {
				while (!(Thread.currentThread().isInterrupted() || toClient.checkError() || disconnectSilently)) {
					this.wait(400);
					// Write to client
					while (messageToClient.size() > 0) {
						String msg = messageToClient.poll();
						toClient.println(msg);
					}
					// Read from client
					String readLine;
					while (fromClient.ready() && (readLine = fromClient.readLine()) != null) {
						this.messageFromClient.offer(readLine);
					}
					// Tell controller if we received message
					if (messageFromClient.size() > 0) {
						controller.gotMessageFromClient(this);
					}
				}
			}
		} catch (InterruptedException e) {
			System.out.println(name + " was interrupted, exiting.");
		} catch (IOException e) {
			if (!disconnectSilently) {
				System.err.println(this.name + " got an exception.");
				e.printStackTrace();
			}
		}
		if (goodbyeMessage != null) {
			toClient.println(goodbyeMessage);
		}
		System.out.printf("Closing thread \"%s\"\n", name);
		if (!disconnectSilently) {
			tellOtherWeClosed();
		}
		closeResources();
		controller.removeClient(myObservableClient, disconnectSilently);
		Threads.THREADS.remove(Thread.currentThread());
	}

	private void tellOtherWeClosed() {
		// If client socket is still alive, tell it we closed
		if (!toClient.checkError()) {
			ConnectionClosed cc = new ConnectionClosed();
			String json = new Gson().toJson(cc);
			toClient.println(json);
		}
	}

	/**
	 * Close the connection without telling the other side. Usually done if the
	 * other side sent us a close connection request.
	 */
	public void closeSilently() {
		synchronized (this) {
			disconnectSilently = true;
			this.notifyAll();
		}
	}

	public void closeWithMessage(String goodbyeMessage) {
		synchronized (this) {
			this.goodbyeMessage = goodbyeMessage;
			this.disconnectSilently = true;
			this.notifyAll();
		}
	}

	private void closeHelper(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
			}
		}
	}

	private void closeResources() {
		closeHelper(toClient);
		closeHelper(fromClient);
		closeHelper(clientSocket);
	}

	public String getName() {
		return name;
	}

	public void sendMessage(String msg) {
		synchronized (this) {
			messageToClient.offer(msg);
			this.notifyAll();
		}
	}

	public String getMessage() {
		synchronized (this) {
			return messageFromClient.poll();
		}
	}

}
