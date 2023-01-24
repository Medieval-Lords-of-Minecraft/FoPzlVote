package me.fopzl.vote.shared.io;

public class VoteStatsLocal {
	boolean needToSave;

	private static long streakLimit; // votes
	private static long streakResetTime; // days
	int voteStreak, votesQueued, votesQueuedNoStreak; // current
	
	public static void setStreakLimit(long numVotes) {
		streakLimit = numVotes;
	}
	
	public static void setStreakResetTime(long numDays) {
		streakResetTime = numDays;
	}
	
	public static long getStreakLimit() {
		return streakLimit;
	}
	
	public static long getStreakResetTime() {
		return streakResetTime;
	}
	
	public VoteStatsLocal() {
		needToSave = true;
		
		voteStreak = 0;
		votesQueued = 0;
		votesQueuedNoStreak = 0;
	}
	
	public VoteStatsLocal(int totalVotes, int voteStreak, int votesQueued, int votesQueuedNoStreak) {
		needToSave = false;
		
		this.voteStreak = voteStreak;
		this.votesQueued = votesQueued;
		this.votesQueuedNoStreak = votesQueuedNoStreak;
	}
	
	public void addVote(String site) {
		needToSave = true;
	}
	
	public int getStreak() {
		return voteStreak;
	}
	
	public int getVotesQueued(boolean countsTowardsStreak) {
		return countsTowardsStreak ? votesQueued : votesQueuedNoStreak;
	}
}
