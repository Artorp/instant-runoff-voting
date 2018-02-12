package no.artorp.instantrunoffvoting.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import no.artorp.instantrunoffvoting.Alerter;
import no.artorp.instantrunoffvoting.globals.ResourceLocations;
import no.artorp.instantrunoffvoting.globals.Threads;
import no.artorp.instantrunoffvoting.json_models.CandidatesBallot;
import no.artorp.instantrunoffvoting.json_models.MessageType;
import no.artorp.instantrunoffvoting.json_models.VotingLocked;
import no.artorp.instantrunoffvoting.server.ObservableClient.State;
import no.artorp.instantrunoffvoting.utils.GetPackageHeader;
import no.artorp.instantrunoffvoting.utils.RunOnJavafx;
import no.artorp.instantrunoffvoting.utils.TimedTask;

public class HostLobbyController {

	private Stage myStage;
	private VotingServer votingServer;
	private Thread votingServerThread;

	// FXML fields
	@FXML
	private Label labelServerName;
	@FXML
	private TextField textfieldIp;
	@FXML
	private TextField textfieldPort;

	@FXML
	private ListView<String> listviewCandidates;
	private ObservableList<String> candidates = FXCollections.observableArrayList();
	@FXML
	private Button buttonChangeCandidates;

	@FXML
	private TableView<ObservableClient> tableviewClients;
	@FXML
	private TableColumn<ObservableClient, String> tablecolumnName;
	@FXML
	private TableColumn<ObservableClient, String> tablecolumnStatus;
	private ObservableList<ObservableClient> clients = FXCollections.observableArrayList();
	@FXML
	private Button buttonKick;
	@FXML
	private Button buttonRename;
	@FXML
	private Label labelBallotsStatus;
	
	@FXML
	private TextArea textareaUpdates;
	@FXML
	private CheckBox checkboxAutoscroll;
	@FXML
	private CheckBox checkboxOnlyConnections;

	@FXML
	private Button buttonLock;
	@FXML
	private Button buttonBeginVoting;

	private ServerSocket serverSocket;
	private String serverName;
	private static final String BALLOT_STATUS = "%s of %s clients have submitted their ballots";
	private int votingLocked = 0; // 0 - not, 1 - gui locked, ballots accepted, 2 - ballots rejected
	private StatusUpdates statusUpdates = new StatusUpdates();

	public HostLobbyController(Stage stage, ServerSocket serverSocket, String serverName) {
		this.myStage = stage;
		this.serverSocket = serverSocket;
		this.serverName = serverName;
	}

	public void initialize() {
		System.out.println("HostLobbyController");
		System.out.println("Local port: " + serverSocket.getLocalPort());

		labelServerName.setText(serverName);

		// Attempt to retrieve external IP from Amazon AWS
		String ip_text = "unknown";
		try (Scanner sc = new Scanner(new URL("http://checkip.amazonaws.com/").openStream(), "UTF-8")) {
			sc.useDelimiter("\\A"); // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
			if (sc.hasNext()) {
				ip_text = sc.next();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		textfieldPort.setText(String.valueOf(serverSocket.getLocalPort()));
		textfieldIp.setText(ip_text);

		// Setup candidate listview
		listviewCandidates.setItems(candidates);

		buttonChangeCandidates.setOnAction(ae -> {
			// Show a textbox with all candidates
			// Check if any connected clients got a list of candidates
			boolean clientGotList = false;
			for (ObservableClient c : clients)
				if (c.isReceivedCandidates()) {
					clientGotList = true;
					break;
				}
			if (clientGotList) {
				if (!askUser("One or more clients have already received a list of clients."
						+ " Are you sure you want to change it?"))
					return;
			}
			// Setup candidate edit stage
			Stage s = new Stage(StageStyle.UTILITY);
			FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourceLocations.FXML_EDIT_CANDIDATES));
			EditCandidatesController controller = new EditCandidatesController(s, this.candidates.iterator());
			loader.setController(controller);
			Parent root;
			try {
				root = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
				Alerter.showAlertError(e.getMessage());
				return;
			}
			s.initOwner(myStage);
			s.setScene(new Scene(root));
			s.showAndWait();
			if (!controller.exitedSuccessfully())
				return;
			// Check if new list has changes to candidate list
			List<String> newCandidates = controller.getCandidates();
			List<String> oldCandidates = new ArrayList<>(candidates);
			this.candidates.clear();
			this.candidates.addAll(newCandidates);
			if (utilListsEqual(oldCandidates, newCandidates)) {
				System.out.println("The new list of candidates is identical to the old list");
				return;
			}
			// Send the new list of candidates to all clients
			for (ObservableClient c : clients) {
				c.sendCandidateList(newCandidates);
			}
			newUpdate("Host changed candidates", false);
			stateChanged();
		});

		// Create a new thread that will handle all new connections
		votingServer = new VotingServer(this, serverSocket, serverName);
		votingServerThread = new Thread(votingServer, "ListenerServerThread");
		votingServerThread.start();
		Threads.THREADS.add(votingServerThread);

		// Set up tableview for clients
		tableviewClients.setItems(clients);
		tablecolumnName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tablecolumnStatus.setCellValueFactory(new PropertyValueFactory<>("state"));

		// Client Buttons
		tableviewClients.getSelectionModel().selectedItemProperty().addListener((obs, oldvalue, newvalue) -> {
			if (newvalue == null) {
				buttonKick.setDisable(true);
				buttonRename.setDisable(true);
			} else {
				buttonKick.setDisable(false);
				buttonRename.setDisable(false);
			}
		});
		buttonKick.setDisable(true);
		buttonRename.setDisable(true);
		tableviewClients.getSelectionModel().selectFirst();
		buttonKick.setOnAction(ae -> {
			ObservableClient c = tableviewClients.getSelectionModel().getSelectedItem();
			if (c == null)
				return;
			System.out.println("Kicking " + c.getName());
			newUpdate("The host kicked " + c.getName(), true);
			c.kick();
			removeClient(c, true);
		});
		buttonRename.setOnAction(ae -> {
			ObservableClient c = tableviewClients.getSelectionModel().getSelectedItem();
			if (c == null)
				return;
			// Show a text query to host
			TextInputDialog dialog = new TextInputDialog(c.getName());
			Optional<String> result = dialog.showAndWait();
			if (!result.isPresent())
				return;
			if (result.get().equals(c.getName()))
				return;
			String newName = result.get();
			String updateText = "Renaming " + c.getName() + " to " + newName;
			System.out.println(updateText);
			newUpdate(updateText, false);
			c.rename(newName);
		});

		stateChanged();
		
		checkboxOnlyConnections.selectedProperty().addListener((obs, oldval, newval) -> {
			if (newval != null) {
				updateUpdates();
			}
		});

		buttonBeginVoting.setDisable(true);
		buttonLock.setOnAction(ae -> {
			// Disable / Enable
			buttonLock.setDisable(true);
			double delayLength = 400; // in milliseconds
			if (votingLocked == 1) {
				return;
			} else if (votingLocked == 0) {
				// lock the votes!
				// First, check if everyone has submitted their votes
				int[] stats = submittedCount();
				int hasSubmitted = stats[0];
				int total = stats[1];
				if (hasSubmitted < total) {
					// Ask for confirmation
					boolean confirm = askUser(String.format("%s of %s clients have not submitted their ballots."
							+ " Are you sure you want to lock submissions?", total - hasSubmitted, total));
					if (!confirm) {
						TimedTask.runLater(Duration.millis(200), () -> buttonLock.setDisable(false));
						return;
					}
				}
				votingLocked = 1;
				buttonLock.setText("Locking voting...");
				newUpdate("Host locked voting", false);
				TimedTask.runLater(Duration.millis(delayLength), () -> {
					buttonLock.setText("Allow new ballots from clients");
					votingLocked = 2;
					buttonLock.setDisable(false);
					buttonBeginVoting.setDisable(false);
				});
				freezeClients(true);
			} else {
				// Unlock the votes!
				buttonBeginVoting.setDisable(true);
				votingLocked = 1;
				buttonLock.setText("Unlocking voting...");
				newUpdate("Host unlocked voting", false);
				TimedTask.runLater(Duration.millis(delayLength), () -> {
					buttonLock.setText("Close voting");
					votingLocked = 0;
					buttonLock.setDisable(false);
				});
				freezeClients(false);
			}
		});
		
		buttonBeginVoting.setOnAction(ae -> {
			// Collect ballots
			List<String> candidateNames = new ArrayList<>(candidates);
			List<List<String>> ballots = new ArrayList<>();
			Set<String> ourCandidates = new HashSet<>(candidateNames);
			for (ObservableClient oc : clients) {
				if (oc.getState() == State.HAS_SUBMITTED) {
					List<String> ballot = new ArrayList<>(oc.getSubmittedBallot());
					// Compare set of candidates with our candidates
					Set<String> theirCandidates = new HashSet<>(ballot);
					if (!ourCandidates.equals(theirCandidates)) {
						// INVALID BALLOT
						// This shouldn't happen, ballots are verified once they are received
						System.err.printf("%s had an invalid ballot! Rejected ballot:\n%s\n", oc.getName(), ballot);
						oc.sendVoteStartInvalidBallot(candidateNames);
						continue;
					}
					ballots.add(ballot);
					// Tell client their ballot was collected
					oc.sendVoteStartBallotCollected();
				} else {
					// Tell client we started voting but they didn't participate
					oc.sendVoteStartNoBallotCollected();
				}
			}
			// This is it. Begin voting.
			FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourceLocations.FXML_RESULTS));
			ResultsController controller = new ResultsController(candidateNames, ballots);
			loader.setController(controller);
			Parent root;
			try {
				root = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
				Alerter.showAlertError("An unexpected error occured while loading the Results UI");
				return;
			}
			Scene s = new Scene(root);
			Stage resultStage = new Stage();
			resultStage.initOwner(myStage);
			resultStage.setScene(s);
			resultStage.showAndWait();
		});
	}

	private boolean askUser(String question) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setHeaderText(null);
		alert.setContentText(question);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			return true;
		}
		return false;
	}

	private <T extends Comparable<? super T>> boolean utilListsEqual(List<T> a, List<T> b) {
		if (a.size() != b.size())
			return false;
		List<T> a_copy = new ArrayList<>(a);
		List<T> b_copy = new ArrayList<>(b);
		Collections.sort(a_copy);
		Collections.sort(b_copy);
		Iterator<T> a_iter = a_copy.iterator();
		Iterator<T> b_iter = b_copy.iterator();
		while (a_iter.hasNext() && b_iter.hasNext()) {
			T ao = a_iter.next();
			T bo = b_iter.next();
			if (!ao.equals(bo))
				return false;
		}
		return true;
	}

	/**
	 * One or more clients have updated their status, or connected / disconnected
	 */
	public void stateChanged() {
		// Use runnable to ensure thread safety / JavaFX GUI execution
		RunOnJavafx.run(() -> {
			// Update label
			int[] stats = submittedCount();
			int hasSubmitted = stats[0];
			int total = stats[1];
			labelBallotsStatus.setText(String.format(BALLOT_STATUS, hasSubmitted, total));
		});
	}

	/**
	 * @return an intarray of size 2, first element is number of submission, second
	 *         is total clients connected
	 */
	private int[] submittedCount() {
		int total = clients.size();
		int hasSubmitted = 0;
		for (ObservableClient c : clients) {
			if (c.getState() == State.HAS_SUBMITTED)
				hasSubmitted++;
		}
		return new int[] { hasSubmitted, total };
	}

	/**
	 * Called when a new client has been connected
	 * 
	 * @param client
	 *            the new client
	 */
	public void offerClient(ObservableClient client) {
		RunOnJavafx.run(() -> {
			String newConnection = client.getName() + " connected.";
			System.out.println(newConnection);
			newUpdate(newConnection, true);
			clients.add(client);
			// Send welcome message
			client.sendWelcomeMessage(serverName);
			// If candidates done, send list of candidates
			if (candidates.size() > 0) {
				List<String> candidateNames = new ArrayList<>(candidates);
				client.sendCandidateList(candidateNames);
			}
			// If locked, send locked status
			if (votingLocked >= 1) {
				client.sendLocked(true);
			}
			stateChanged();
		});
	}

	public void removeClient(ObservableClient client, boolean silent) {
		RunOnJavafx.run(() -> {
			if (!clients.contains(client))
				return;
			if (!silent) {
				String didDisconnect = client.getName() + " disconnected.";
				System.out.println(didDisconnect);
				newUpdate(didDisconnect, true);
			}
			clients.remove(client);
			stateChanged();
		});
	}

	public void gotMessageFromClient(ClientThread client) {
		RunOnJavafx.run(() -> {
			String msg;
			while ((msg = client.getMessage()) != null) {
				System.out.printf("Got message from %s: %s\n", client.getName(), msg);
				JsonElement el = new JsonParser().parse(msg);
				MessageType type = GetPackageHeader.getPackageHeader(el);
				if (type == null) {
					System.out.println("Unknown packageheader");
					continue;
				}
				ObservableClient clientObs = client.getObservableClient();
				// Note to self, switch would be better, but IDE has autocomplete problems
				if (type == MessageType.CLOSED) {
					// Client closed their connection
					client.closeSilently();
					removeClient(clientObs, false);
				} else if (type == MessageType.BALLOT) {
					// They submitted their ballot
					System.out.println(client.getName() + " submitted their ballot:");
					System.out.println(msg);
					if (clientObs.getState() == State.HAS_SUBMITTED) {
						newUpdate(client.getName() + " resubmitted their ballot.", false);
					} else {
						newUpdate(client.getName() + " submitted their ballot.", false);
					}
					// Verify gui is not locked
					if (votingLocked == 2) {
						// Reject the message
						// Maybe retain the message in the future?
						System.out.println("But voting is locked, ballot rejected");
						newUpdate("But voting is locked, ballot rejected", false);
						return;
					}
					CandidatesBallot cb = new Gson().fromJson(el, CandidatesBallot.class);
					List<String> candidateNames = cb.getCandidatesOrdered();
					// Verify the list is in sync with out candidates
					boolean trouble = false;
					if (candidates.size() != candidateNames.size())
						trouble = true;
					else {
						// Compare elements
						Set<String> ourNames = new HashSet<>(candidates);
						Set<String> theirNames = new HashSet<>(candidateNames);
						if (!ourNames.equals(theirNames))
							trouble = true;
					}
					if (trouble) {
						// This shouldn't happen. Their list of ballots do not fit ours.
						clientObs.sendResyncCandidateList(candidateNames);
						stateChanged();
						return;
					}
					clientObs.setState(State.HAS_SUBMITTED);
					clientObs.setSubmittedBallot(candidateNames);
					stateChanged();
				}
			}
		});
	}

	/**
	 * Send a message to all connected clients to disable or enable submit button
	 * 
	 * @param freeze
	 *            {@code true} if they should disable send votes, {@code false}
	 *            otherwise
	 */
	private void freezeClients(boolean freeze) {
		String json = new Gson().toJson(new VotingLocked(freeze));
		for (ObservableClient c : clients) {
			c.sendMessage(json);
		}
	}
	
	private void newUpdate(String updateText, boolean isConnection) {
		statusUpdates.addUpdate(updateText, isConnection);
		updateUpdates();
	}
	
	private void updateUpdates() {
		// backup selection and scroll
		IndexRange prevSelection = textareaUpdates.getSelection();
		double prevScrollTop = textareaUpdates.getScrollTop();
		if (checkboxOnlyConnections.isSelected()) {
			textareaUpdates.setText(statusUpdates.connectionsOnly());
		} else {
			textareaUpdates.setText(statusUpdates.toString());
		}
		if (checkboxAutoscroll.isSelected()) {
			// Must use runlater to ensure proper scroll down
			textareaUpdates.setScrollTop(prevScrollTop);
			TimedTask.runLater(Duration.millis(30), () -> {
				textareaUpdates.setScrollTop(Double.MAX_VALUE);
			});
		} else {
			textareaUpdates.setScrollTop(prevScrollTop);
		}
		if (prevSelection.getLength() > 0) {
			textareaUpdates.selectRange(prevSelection.getStart(), prevSelection.getEnd());
		}
	}
}
