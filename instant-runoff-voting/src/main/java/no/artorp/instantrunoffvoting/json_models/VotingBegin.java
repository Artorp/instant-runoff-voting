package no.artorp.instantrunoffvoting.json_models;

import java.util.List;

/**
 * The voting begins, let the client know how they participated.
 * 
 * Sent from server to client once the voting begins.
 */
public class VotingBegin extends JsonPackage {

	private VotingParticipation votingParticipation;
	private List<String> candidates;

	/**
	 * Inform the client that the voting began.
	 * 
	 * @param votingParticipation
	 *            how the client participated
	 */
	public VotingBegin(VotingParticipation votingParticipation) {
		messageType = MessageType.VOTING_BEGIN;
		this.votingParticipation = votingParticipation;
		this.candidates = null;
	}

	/**
	 * Send the set of candidates with the update package. Used if the ballot was
	 * invalid. The client will only use the list of candidates if the ballot was
	 * invalid.
	 * 
	 * @param votingParticipation
	 *            how the client participated in the voting. Invalid in this case.
	 * @param candidates
	 *            an updated list of candidates
	 */
	public VotingBegin(VotingParticipation votingParticipation, List<String> candidates) {
		this(votingParticipation);
		this.candidates = candidates;
	}

	public VotingParticipation getVotingParticipation() {
		return votingParticipation;
	}

	public List<String> getCandidates() {
		return candidates;
	}

}
