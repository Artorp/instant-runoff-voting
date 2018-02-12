package no.artorp.instantrunoffvoting.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;
import no.artorp.instantrunoffvoting.Alerter;
import no.artorp.instantrunoffvoting.globals.Threads;
import no.artorp.instantrunoffvoting.json_models.CandidatesBallot;
import no.artorp.instantrunoffvoting.json_models.CandidatesPackage;
import no.artorp.instantrunoffvoting.json_models.CandidatesSyncError;
import no.artorp.instantrunoffvoting.json_models.MessageType;
import no.artorp.instantrunoffvoting.json_models.VotingBegin;
import no.artorp.instantrunoffvoting.json_models.VotingLocked;
import no.artorp.instantrunoffvoting.json_models.VotingParticipation;
import no.artorp.instantrunoffvoting.json_models.WasRenamed;
import no.artorp.instantrunoffvoting.json_models.WelcomeMessage;
import no.artorp.instantrunoffvoting.utils.GetPackageHeader;
import no.artorp.instantrunoffvoting.utils.RunOnJavafx;
import no.artorp.instantrunoffvoting.utils.TimedTask;

public class ClientLobbyController {

	@FXML
	Label labelServerName;
	@FXML
	Label labelSecretName;
	@FXML
	ListView<String> listviewCandidates;
	ObservableList<String> candidates = FXCollections.observableArrayList();
	@FXML
	Button buttonShowHideNames;
	@FXML
	Text textStatus;
	@FXML
	Button buttonSendRank;
	@FXML
	BooleanProperty lockedVoting = new SimpleBooleanProperty(false);

	// Strings
	private static final String SEND_TEXT_DEFAULT = "Submit candidates";
	private static final String SEND_TEXT_LOCKED = "Candidate submission locked by server";

	private final BooleanProperty hideCandidateNames = new SimpleBooleanProperty(false);
	private ClientSocketHandler socketHandler;

	public ClientLobbyController(ClientSocketHandler socketHandler) {
		this.socketHandler = socketHandler;
		this.socketHandler.setController(this);
	}

	public void initialize() {
		System.out.println("ClientLobbyController");

		// Secret name
		Background blackBackground = new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY));
		labelSecretName.setBackground(blackBackground);
		labelSecretName.setOnMouseEntered(me -> {
			me.consume();
			labelSecretName.setBackground(null);
		});
		labelSecretName.setOnMouseExited(me -> {
			me.consume();
			labelSecretName.setBackground(blackBackground);
		});

		// Listview
		listviewCandidates.setItems(candidates);

		// Show / Hide candidate names
		buttonShowHideNames.setOnAction(ae -> {
			hideCandidateNames.set(hideCandidateNames.get() == false);
		});
		hideCandidateNames.addListener((obs, oldval, newval) -> {
			if (newval.booleanValue()) {
				listviewCandidates.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
					@Override
					public ListCell<String> call(ListView<String> param) {
						return new ListCell<String>() {
							@Override
							protected void updateItem(String item, boolean empty) {
								// Always call the super method, very important
								super.updateItem(item, empty);

								if (item == null || empty) {
									setText(null);
								} else {
									setText("Names are hidden!");
								}
							}

						};
					}
				});
			} else {
				listviewCandidates.setCellFactory(param -> new DragableListCell());
			}
		});
		listviewCandidates.setCellFactory(param -> new DragableListCell());

		// If the server locks the GUI, disable send button
		lockedVoting.addListener((obs, oldv, newv) -> {
			if (newv != null) {
				buttonSendRank.setDisable(newv);
				buttonSendRank.setText(newv ? SEND_TEXT_LOCKED : SEND_TEXT_DEFAULT);
			}
		});

		textStatus.setText("Waiting for handshake from server.");

		// Submit candidates
		buttonSendRank.setOnAction(ae -> {
			if (candidates.size() == 0)
				return;
			buttonSendRank.setDisable(true);
			List<String> orderedNames = new ArrayList<>(candidates);
			CandidatesBallot cb = new CandidatesBallot(orderedNames);
			String json = new Gson().toJson(cb);
			socketHandler.sendMessageToServer(json);
			textStatus.setText("You sent your ballot to the server. You may still resend until the voting closes."
					+ " Do not disconnect, only ballots for connected clients are counted!");
			textStatus.setFill(Color.BLACK);
			TimedTask.runLater(Duration.millis(500), () -> {
				if (!lockedVoting.get()) {
					buttonSendRank.setDisable(false);
				}
			});
		});

		// Start the socket thread
		Thread t = new Thread(socketHandler, "ClientSocketThread");
		t.start();
		Threads.THREADS.add(t);
	}

	public void gotMessageFromServer() {
		RunOnJavafx.run(() -> {
			String msg;
			while ((msg = socketHandler.getMessage()) != null) {
				System.out.println("Got message from server:\n" + msg);
				// Decode JSON
				JsonElement jsonElement = new JsonParser().parse(msg);
				MessageType type = GetPackageHeader.getPackageHeader(jsonElement);
				Gson gson = new Gson();

				switch (type) {
				case WELCOME:
					// Welcome message
					WelcomeMessage wm = gson.fromJson(jsonElement, WelcomeMessage.class);
					labelServerName.setText(wm.getServerName());
					labelSecretName.setText(wm.getClientName());
					textStatus.setText("The host is currently assigning candidates.");
					textStatus.setFill(Color.BLACK);
					break;

				case CANDIDATES:
					// A list of candidates
					CandidatesPackage cp = gson.fromJson(jsonElement, CandidatesPackage.class);
					int oldSize = candidates.size();
					newCandidateList(cp.getCandidates());
					if (oldSize == 0) {
						textStatus.setText("The host is currently waiting for your submission.");
						textStatus.setFill(Color.BLACK);
					} else {
						textStatus.setText("The host changed the candidates. Please reorder and resubmit.");
						textStatus.setFill(Color.RED);
					}
					
					break;

				case SYNC_ERROR:
					// Sync error after submitting our ballots
					// Got new list of ballots
					CandidatesSyncError cse = gson.fromJson(jsonElement, CandidatesSyncError.class);
					newCandidateList(cse.getCandidates());
					textStatus.setText("The host is currently waiting for your submission.");
					textStatus.setFill(Color.BLACK);
					Alerter.showAlertError("An unexpected error occured:\n"
							+ "Your ballot didn't match the server list, please reorder and resubmit");
					break;

				case LOCKED:
					// Whether the GUI is locked or not
					VotingLocked vl = gson.fromJson(jsonElement, VotingLocked.class);
					lockedVoting.setValue(vl.isLocked());
					break;

				case CLOSED:
					// The server closed our connection
					textStatus.setText("The remote host closed the connection.");
					textStatus.setFill(Color.RED);
					lockedVoting.set(true);
					socketHandler.disconnectSilently();
					break;

				case VOTING_BEGIN:
					// The server began the voting process
					// Did we participate?
					VotingBegin vb = gson.fromJson(jsonElement, VotingBegin.class);
					if (vb.getVotingParticipation() == VotingParticipation.BALLOT_COLLECTED) {
						textStatus.setText("The server began voting. Our submission was collected."
								+ " It is now safe to disconnect.");
						textStatus.setFill(Color.BLACK);
					} else if (vb.getVotingParticipation() == VotingParticipation.NO_BALLOT_COLLECTED) {
						textStatus.setText("The server began voting. We did not participate in the voting."
								+ " It is now safe to disconnect.");
						textStatus.setFill(Color.RED);
					} else if (vb.getVotingParticipation() == VotingParticipation.INVALID_BALLOT) {
						List<String> candidates = vb.getCandidates();
						textStatus.setText("The server began voting. Our ballot was rejected as invalid."
								+ " Updating list of candidates."
								+ " It is now safe to disconnect.");
						textStatus.setFill(Color.RED);
						if (candidates == null) {
							return;
						}
						newCandidateList(candidates);
					}
					break;
					
				case RENAMED:
					// The server renamed us
					WasRenamed wr = new Gson().fromJson(jsonElement, WasRenamed.class);
					String newName = wr.getNewName();
					labelSecretName.setText(newName);
					break;
				
				case KICKED:
					// We were kicked from the server
					textStatus.setText("Kicked from server");
					textStatus.setFill(Color.RED);
					lockedVoting.set(true);
					socketHandler.disconnectSilently();
					break;

				default:
					System.out.println("Unknown message type received: " + type);
					System.out.println("Message was: " + msg);
					break;
				}
			}
		});
	}
	
	private void newCandidateList(List<String> newCandidateNames) {
		System.out.println("Got new list of candidates:\n" + newCandidateNames);
		Collections.shuffle(newCandidateNames);
		candidates.clear();
		candidates.addAll(newCandidateNames);
	}

}
