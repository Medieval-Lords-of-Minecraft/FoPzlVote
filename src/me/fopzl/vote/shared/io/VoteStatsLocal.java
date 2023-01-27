package me.fopzl.vote.shared.io;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import me.neoblade298.neocore.bukkit.NeoCore;

import java.util.Map.Entry;

public class VoteStatsLocal {
	private boolean dirty = false;

	private static long streakLimit; // votes
	private static long streakResetTime; // days
	private UUID uuid;
	private int voteStreak = 0, votesQueued = 0; // current
	private ArrayList<OldVoteStreak> oldVoteStreaks; // Unrewarded previous vote streaks
	
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
	
	public VoteStatsLocal(UUID uuid) {
		this.uuid = uuid;
	}
	
	public VoteStatsLocal(UUID uuid, int voteStreak, int votesQueued) {
		this.uuid = uuid;
		this.voteStreak = voteStreak;
		this.votesQueued = votesQueued;
	}
	
	public void addVote() {
		
	}
	
	public int getStreak() {
		return voteStreak;
	}
	
	public ArrayList<OldVoteStreak> getOldVoteStreaks() {
		return oldVoteStreaks;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void save(Statement insert, Statement delete) {
		try {
			insert.addBatch("replace into fopzlvote_playerStats values ('" + uuid + "','" + NeoCore.getInstanceKey() + "'," + voteStreak  + "," + votesQueued + ");");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
