package me.fopzl.vote.bukkit.io;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.fopzl.vote.bukkit.BukkitVote;
import me.neoblade298.neocore.bukkit.NeoCore;

public class VoteStats {
	private static int streakLimit = 0;
	private static int streakResetTime = 0; // In days
	
	private UUID uuid;
	private int totalVotes; // ever
	private int voteStreak = 0, votesQueued = 0;
	private LocalDateTime lastVoted;
	private HashMap<VoteMonth, Map<String, Integer>> monthlySiteCounts; // value is <voteSite, votes this month>
	private boolean dirty;
	private ArrayList<OldVoteStreak> oldStreaks = new ArrayList<OldVoteStreak>();
	
	public VoteStats(UUID uuid) {
		this.uuid = uuid;
		totalVotes = 0;
		lastVoted = LocalDateTime.of(0, 1, 1, 0, 0);
		setMonthlySiteCounts(new HashMap<VoteMonth, Map<String, Integer>>());
	}
	
	public VoteStats(UUID uuid, int totalVotes, int voteStreak, int votesQueued, LocalDateTime lastVoted) {
		this.uuid = uuid;
		this.totalVotes = totalVotes;
		this.lastVoted = lastVoted;
		this.votesQueued = votesQueued;
		setMonthlySiteCounts(new HashMap<VoteMonth, Map<String, Integer>>());
	}
	
	public LocalDateTime getLastVoted() {
		return lastVoted;
	}
	
	// Assume this method is called async
	public void handleVote(String site) {
		totalVotes++;
		
		if (BukkitVote.debug) {
			Bukkit.getLogger().info("[FoPzlVote] Handled vote for player " + uuid + ", total votes " + totalVotes);
		}
		
		// Check for streak loss
		LocalDateTime time = LocalDateTime.now();
		if (Duration.between(lastVoted, time).compareTo(Duration.ofDays(streakResetTime)) > 0) {
			Bukkit.getLogger().info("[FoPzlVote] Player " + uuid + " lost streak, last vote was " + lastVoted + ", now is " + time + ". Old vote streak " + voteStreak + ", queued " + votesQueued);
			oldStreaks.add(new OldVoteStreak(voteStreak, votesQueued));
			voteStreak = 0;
			votesQueued = 0;
		}
		
		// Monthly vote counts
		lastVoted = time;
		VoteMonth now = new VoteMonth(LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue());
		Map<String, Integer> currCounts = monthlySiteCounts.getOrDefault(now, new HashMap<String, Integer>());
		currCounts.put(site, currCounts.getOrDefault(site, 0) + 1);
		monthlySiteCounts.putIfAbsent(now, currCounts);
		
		// If our votes are caught up, reward votes as normal
		if (voteStreak  == votesQueued && oldStreaks.isEmpty() && Bukkit.getPlayer(uuid) != null) {
			
		}
		// Otherwise just increase the streak (Waiting for login to kick in and reward all votes at once)
		else {
			votesQueued++;
		}
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
			insert.addBatch("replace into fopzlvote_playerQueue values ('" + uuid + "', '" + NeoCore.getInstanceKey() + "'," + voteStreak + "," + votesQueued + ");");
			
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
	
	public void addStreak(OldVoteStreak streak) {
		oldStreaks.add(streak);
	}
	
	public ArrayList<OldVoteStreak> getStreaks() {
		return oldStreaks;
	}
}
