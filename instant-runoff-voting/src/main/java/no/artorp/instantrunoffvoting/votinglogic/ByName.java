package no.artorp.instantrunoffvoting.votinglogic;

import java.util.Comparator;

public class ByName implements Comparator<CandidateCount> {

	@Override
	public int compare(CandidateCount o1, CandidateCount o2) {
		return o1.getName().compareTo(o2.getName());
	}

}
