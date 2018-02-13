package no.artorp.instantrunoffvoting.votinglogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VotingSimulator {

	private final List<String> candidates;
	private final List<List<String>> ballots;

	private final List<String> rounds = new ArrayList<>();

	public VotingSimulator(List<String> candidates, List<List<String>> ballots) {
		this.candidates = candidates;
		this.ballots = ballots;
	}

	public void runSimulation() {
		Collections.sort(candidates);
		List<String> stillInIt = new ArrayList<>(candidates);
		boolean winnerChosen = false;
		while (!winnerChosen) {
			StringBuilder sb = new StringBuilder();

			sb.append(String.format("%s candidates and %s ballots\n\n", stillInIt.size(), ballots.size()));
			sb.append("Number of first vote per candidate:\n");

			// count first votes
			Map<String, Integer> firstVotes = new HashMap<>();
			for (String c : stillInIt) {
				firstVotes.put(c, 0);
			}
			for (List<String> ballot : ballots) {
				String candidate_firstvote = ballot.get(0);
				firstVotes.put(candidate_firstvote, firstVotes.get(candidate_firstvote) + 1);
			}
			// Create list of candidates
			List<CandidateCount> candidateCounts = new ArrayList<>();
			for (String c : firstVotes.keySet()) {
				System.out.printf("%s: %s\n", c, firstVotes.get(c));
				CandidateCount cc = new CandidateCount(c, firstVotes.get(c));
				candidateCounts.add(cc);
			}
			System.out.println();

			// Output in order of name
			Collections.sort(candidateCounts, new ByName());
			for (CandidateCount cc : candidateCounts) {
				sb.append(String.format("%s: %s\n", cc.getName(), cc.getVotes()));
			}
			sb.append('\n');

			// Get all tied winners and all tied losers
			Collections.sort(candidateCounts);

			int totalVotes = ballots.size();
			List<CandidateCount> winners = getWinners(candidateCounts);
			List<CandidateCount> losers = getLosers(candidateCounts);

			int winVotes = winners.get(0).getVotes();
			int loseVotes = losers.get(0).getVotes();
			double winPercent = 100d * (double) winVotes / (double) totalVotes;
			double losePercent = 100d * (double) loseVotes / (double) totalVotes;

			CandidateCount loser = null;

			if (winners.size() == 1) {
				sb.append(winners.get(0).getName()).append(" has ");
			} else {
				sb.append(winners.size()).append(" candidates have ");
			}
			sb.append(String.format("the highest number of votes with %s votes (%.2f%%)\n", winVotes, winPercent));
			
			if (losers.size() == 1) {
				sb.append(losers.get(0).getName()).append(" has ");
			} else {
				sb.append(losers.size()).append(" candidates have ");
			}
			sb.append(String.format("the lowest number of votes with %s votes (%.2f%%)\n", loseVotes, losePercent));

			sb.append('\n');
			
			// Check if there is a winner. If so, voting rounds is over
			// use integers to determine if winpercentage is larger than 50 %
			if (winners.size() == 1 && 2 * winVotes > totalVotes) {
				CandidateCount winner = winners.get(0);
				sb.append(String.format("%s won!", winner.getName()));
				winnerChosen = true;
			} else {
				// No winner yet, find loser
				// Check if loser requires tiebreaker
				if (losers.size() > 1) {
					Random r = new Random();
					loser = losers.get(r.nextInt(losers.size()));
					sb.append(String.format("Tiebreaker: %s was randomly selected as the loser of the round.\n", loser.getName()));
				} else {
					loser = losers.get(0);
					sb.append(String.format("\n%s's votes will be redistributed for the next round\n", loser.getName()));
				}
				
				// Redistribute votes by removing all instances of loser
				stillInIt.remove(loser.getName());
				for (List<String> ballot : ballots) {
					ballot.remove(loser.getName());
				}
			}
			
			System.out.println("Winner(s): " + winners);
			System.out.println("Loser: " + loser);

			rounds.add(sb.toString());
		}
	}

	/**
	 * @return a list of strings with readable list of rounds
	 * @throws IllegalStateException
	 *             if called before {@link #runSimulation()}
	 */
	public List<String> getRounds() {
		if (rounds.size() == 0)
			throw new IllegalStateException("Must run simulations before retrieving results");
		return rounds;
	}

	private List<CandidateCount> getLosers(List<CandidateCount> sortedCandidates) {
		List<CandidateCount> losers = new ArrayList<>();
		Integer loserVotes = sortedCandidates.get(0).getVotes();
		for (CandidateCount c : sortedCandidates) {
			if (c.getVotes().equals(loserVotes)) {
				losers.add(c);
			} else {
				break;
			}
		}
		return losers;
	}

	private List<CandidateCount> getWinners(List<CandidateCount> sortedCandidates) {
		List<CandidateCount> winners = new ArrayList<>();
		Integer winnerVotes = sortedCandidates.get(sortedCandidates.size() - 1).getVotes();
		for (int i = sortedCandidates.size() - 1; i >= 0; i--) {
			CandidateCount c = sortedCandidates.get(i);
			if (c.getVotes().equals(winnerVotes)) {
				winners.add(c);
			} else {
				break;
			}
		}
		return winners;
	}

}
