package me.fopzl.vote.bukkit.io;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.fopzl.vote.bukkit.BukkitVote;

public class VoteStats {
	private static int streakLimit = 0;
	private static int streakResetTime = 0; // In days
	
	private UUID uuid;
	private int totalVotes; // ever
	private int voteStreak = 0, votesQueued = 0;
	private LocalDateTime lastVoted;
	private HashMap<VoteMonth, Map<String, Integer>> monthlySiteCounts; // value is <voteSite, votes this month>
	private boolean dirty;
	
	public VoteStats(UUID uuid) {
		this.uuid = uuid;
		totalVotes = 0;
		lastVoted = LocalDateTime.of(0, 1, 1, 0, 0);
		setMonthlySiteCounts(new HashMap<VoteMonth, Map<String, Integer>>());
	}
	
	public VoteStats(UUID uuid, int totalVotes, int voteStreak, LocalDateTime lastVoted) {
		this.uuid = uuid;
		this.totalVotes = totalVotes;
		this.lastVoted = lastVoted;
		setMonthlySiteCounts(new HashMap<VoteMonth, Map<String, Integer>>());
	}
	
	public LocalDateTime getLastVoted() {
		return lastVoted;
	}
	
	public void addVote(String site) {
		totalVotes++;
		
		if (BukkitVote.debug) {
			Bukkit.getLogger().info("[FoPzlVote] Incremented player " + uuid + " total votes to " + totalVotes);
		}
		
		lastVoted = LocalDateTime.now();
		
		VoteMonth now = new VoteMonth(LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue());
		Map<String, Integer> currCounts = getMonthlySiteCounts().getOrDefault(now, new HashMap<String, Integer>());
		currCounts.put(site, currCounts.getOrDefault(site, 0) + 1);
		getMonthlySiteCounts().putIfAbsent(now, currCounts);
	}
	
	public int getTotalVotes() {
		return totalVotes;
	}
	
	public int getVotesThisMonth() {
		VoteMonth now = new VoteMonth(LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue());
		Map<String, Integer> monthVotes = getMonthlySiteCounts().get(now);
		
		if(monthVotes == null) return 0;
		
		int sum = 0;
		for(int votes : monthVotes.values()) {
			sum += votes;
		}
		return sum;
	}
	
	public void save(Statement insert, Statement delete) {
		try {
			insert.addBatch("replace into fopzlvote_playerStats values ('" + uuid + "', " + totalVotes  + ", '" + lastVoted.toString() + "');");
			
			int year = LocalDateTime.now().getYear();
			int month = LocalDateTime.now().getMonthValue();
			VoteMonth now = new VoteMonth(year, month);
			if(month == 1) {
				year--;
				month = 12;
			}
			VoteMonth prev = new VoteMonth(year, month);
			
			// only save this month and the last
			Map<String, Integer> currCounts = monthlySiteCounts.get(now);
			if(currCounts != null) {
				for(Entry<String, Integer> entry : currCounts.entrySet()) {
					insert.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
				}
			}
			
			Map<String, Integer> prevCounts = monthlySiteCounts.get(prev);
			if(prevCounts != null) {
				for(Entry<String, Integer> entry : prevCounts.entrySet()) {
					insert.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public HashMap<VoteMonth, Map<String, Integer>> getMonthlySiteCounts() {
		return monthlySiteCounts;
	}

	public void setMonthlySiteCounts(HashMap<VoteMonth, Map<String, Integer>> monthlySiteCounts) {
		this.monthlySiteCounts = monthlySiteCounts;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public int getStreak() {
		return voteStreak;
	}

	public void setStreak(int streak) {
		this.voteStreak = streak;
	}

	public int getVotesQueued() {
		return votesQueued;
	}

	public void setVotesQueued(int votesQueued) {
		this.votesQueued = votesQueued;
	}

	public static int getStreakLimit() {
		return streakLimit;
	}

	public static void setStreakLimit(int streakLimit) {
		VoteStats.streakLimit = streakLimit;
	}

	public static int getStreakResetTime() {
		return streakResetTime;
	}

	public static void setStreakResetTime(int streakResetTime) {
		VoteStats.streakResetTime = streakResetTime;
	}
}
