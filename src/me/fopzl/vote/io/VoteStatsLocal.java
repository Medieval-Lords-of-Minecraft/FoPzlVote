package me.fopzl.vote.io;

import java.time.LocalDateTime;

public class VoteStatsLocal {
	boolean needToSave;
	
	int voteStreak, votesQueued, votesQueuedNoStreak; // current
	LocalDateTime lastVoted;
	
	public VoteStatsLocal() {
		needToSave = true;
		
		voteStreak = 0;
		votesQueued = 0;
		votesQueuedNoStreak = 0;
		lastVoted = LocalDateTime.of(0, 1, 1, 0, 0);
	}
	
	public VoteStatsLocal(int totalVotes, int voteStreak, int votesQueued, int votesQueuedNoStreak, LocalDateTime lastVoted) {
		needToSave = false;
		
		this.voteStreak = voteStreak;
		this.votesQueued = votesQueued;
		this.votesQueuedNoStreak = votesQueuedNoStreak;
		this.lastVoted = lastVoted;
	}
	
	public void addVote(String site) {
		lastVoted = LocalDateTime.now();
		needToSave = true;
	}
	
	public int getStreak() {
		return voteStreak;
	}
	
	public int getVotesQueued(boolean countsTowardsStreak) {
		return countsTowardsStreak ? votesQueued : votesQueuedNoStreak;
	}
}
