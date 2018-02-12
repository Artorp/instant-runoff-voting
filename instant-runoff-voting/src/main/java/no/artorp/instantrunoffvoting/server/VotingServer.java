package no.artorp.instantrunoffvoting.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import no.artorp.instantrunoffvoting.globals.Threads;

/**
 * Accepts all incoming connections, and assigns them a new client thread
 */
public class VotingServer implements Runnable {

	HostLobbyController controller;
	String serverName;
	ServerSocket mySocket;
	NameFactory nameFactory;

	private final List<ClientThread> clients = new ArrayList<>();

	public VotingServer(HostLobbyController controller, ServerSocket serverSocket, String serverName) {
		this.controller = controller;
		mySocket = serverSocket;
		this.serverName = serverName;
		nameFactory = new NameFactory();
	}

	@Override
	public void run() {
		// Serversocket created prior to this
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Socket newClientSocket = mySocket.accept(); // Has a timeout
				String newName = nameFactory.getNextName();
				System.out.printf("New client connected: %s:%s (%s)\n",
						newClientSocket.getInetAddress().getHostAddress(),
						newClientSocket.getPort(), newName);
				ClientThread r = new ClientThread(controller, newName, newClientSocket);
				ObservableClient clientObs = new ObservableClient(r);
				r.setObservableClient(clientObs);
				controller.offerClient(clientObs);
				Thread t = new Thread(r, newName + " thread");
				t.start();
				Threads.THREADS.add(t);
				addClient(r);
			} catch (SocketTimeoutException e) {
				// Timeout, loop around, check if thread is interrupted
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		closeSockets();
	}

	private void closeSockets() {
		try {
			mySocket.close();
		} catch (IOException e) {
		}
	}

	private void addClient(ClientThread c) {
		synchronized (this) {
			this.clients.add(c);
		}
	}

	/**
	 * Removes the client from the list of clients. Note, the client may still be
	 * connected and running.
	 * 
	 * @param c
	 *            client to be removed
	 * @return {@code true} if client list contained client
	 */
	public boolean removeClient(ClientThread c) {
		synchronized (this) {
			return this.clients.remove(c);
		}
	}

	/**
	 * A thread safe method to get an iterator for all connected clients
	 * 
	 * @return
	 */
	public Iterator<ClientThread> getClients() {
		synchronized (this) {
			return clients.iterator();
		}
	}
}
