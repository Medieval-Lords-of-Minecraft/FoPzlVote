package me.fopzl.vote.shared.io;

public class OldVoteStreak {
	private int voteStreak;
	private int votesQueued;
	public OldVoteStreak(int voteStreak, int votesQueued) {
		this.voteStreak = voteStreak;
		this.votesQueued = votesQueued;
	}
	public int getVoteStreak() {
		return voteStreak;
	}
	public int getVotesQueued() {
		return votesQueued;
	}
}
