package me.fopzl.vote.shared.io;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.fopzl.vote.bukkit.SpigotVote;

public class VoteStatsGlobal {
	private boolean canRemove = false;

	UUID uuid;
	int totalVotes; // ever
	LocalDateTime lastVoted;
	private HashMap<VoteMonth, Map<String, Integer>> monthlySiteCounts; // value is <voteSite, votes this month>
	
	public VoteStatsGlobal(UUID uuid) {
		this.uuid = uuid;
		totalVotes = 0;
		lastVoted = LocalDateTime.of(0, 1, 1, 0, 0);
		setMonthlySiteCounts(new HashMap<VoteMonth, Map<String, Integer>>());
	}
	
	public VoteStatsGlobal(UUID uuid, int totalVotes, int voteStreak, LocalDateTime lastVoted) {
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
		
		if (SpigotVote.debug) {
			Bukkit.getLogger().info("[FoPzlVote] Set player total vote to " + totalVotes);
		}
		
		lastVoted = LocalDateTime.now();
		
		VoteMonth now = new VoteMonth(LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue());
		Map<String, Integer> currCounts = getMonthlySiteCounts().getOrDefault(now, new HashMap<String, Integer>());
		currCounts.put(site, currCounts.getOrDefault(site, 0) + 1);
		getMonthlySiteCounts().putIfAbsent(now, currCounts);
		
		setCanRemove(true);
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
	
	// TODO Might be useless
	/*
	public Runnable getSaveRunnable() {
		return () -> {
			try {
				Statement stmt = BungeeCore.getPluginStatement("FoPzlVote");
				stmt.addBatch("replace into fopzlvote_playerStats values ('" + uuid + "', " + totalVotes  + ", '" + lastVoted.toString() + "');");
				
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
						stmt.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
					}
				}
				
				Map<String, Integer> prevCounts = monthlySiteCounts.get(prev);
				if(prevCounts != null) {
					for(Entry<String, Integer> entry : prevCounts.entrySet()) {
						stmt.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		};
	}
	*/
	
	public boolean isStreakLost() {
		return LocalDateTime.now().isAfter(lastVoted.plusDays(2));
	}

	public boolean canRemove() {
		return canRemove;
	}

	public void setCanRemove(boolean canRemove) {
		this.canRemove = canRemove;
	}

	public HashMap<VoteMonth, Map<String, Integer>> getMonthlySiteCounts() {
		return monthlySiteCounts;
	}

	public void setMonthlySiteCounts(HashMap<VoteMonth, Map<String, Integer>> monthlySiteCounts) {
		this.monthlySiteCounts = monthlySiteCounts;
	}
}