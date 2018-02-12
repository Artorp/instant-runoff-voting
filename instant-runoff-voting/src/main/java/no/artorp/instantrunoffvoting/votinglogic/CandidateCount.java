package no.artorp.instantrunoffvoting.votinglogic;

public class CandidateCount implements Comparable<CandidateCount> {
	private String name;
	private Integer votes;
	
	public CandidateCount(String name, Integer votes) {
		this.name = name;
		this.votes = votes;
	}

	public String getName() {
		return name;
	}

	public Integer getVotes() {
		return votes;
	}

	@Override
	public int compareTo(CandidateCount o) {
		return this.votes.compareTo(o.votes);
	}

	@Override
	public String toString() {
		return name;
	}

}
