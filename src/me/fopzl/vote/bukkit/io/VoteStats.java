package me.fopzl.vote.bukkit.io;

import java.sql.Connection;
import java.sql.ResultSet;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.bukkit.BukkitVote;
import me.fopzl.vote.bukkit.VoteRewards;
import me.neoblade298.neocore.bukkit.NeoCore;

public class VoteStats {
	private static int streakLimit = 0;
	private static int streakResetTime = 0; // In days
	
	private UUID uuid;
	private int totalVotes; // ever
	private int voteStreak = 0, votesQueued = 0;
	private LocalDateTime lastVoted;
	private HashMap<VoteMonth, Map<String, Integer>> monthlySiteCounts = new HashMap<VoteMonth, Map<String, Integer>>();
	private boolean dirty;
	private ArrayList<OldVoteStreak> oldStreaks = new ArrayList<OldVoteStreak>();
	
	public VoteStats(UUID uuid) {
		this.uuid = uuid;
		totalVotes = 0;
		lastVoted = LocalDateTime.now();
	}
	
	public VoteStats(UUID uuid, int totalVotes, LocalDateTime lastVoted) {
		this.uuid = uuid;
		this.totalVotes = totalVotes;
		this.lastVoted = lastVoted;
	}
	
	public LocalDateTime getLastVoted() {
		return lastVoted;
	}
	
	public void handleQueuedVotes() {
		Player p = Bukkit.getPlayer(uuid);
		if (p == null) return;
		
		for (OldVoteStreak old : oldStreaks) {
			Bukkit.getLogger().info("[FoPzlVote] Player " + uuid + " was given old vote streak " + old.getVoteStreak() + " with " + old.getVotesQueued() + " queued");
			VoteRewards.rewardVotes(p, old.getVoteStreak(), old.getVotesQueued());
		}
		oldStreaks.clear();
		new BukkitRunnable() {
			public void run() {
				try (Connection con = NeoCore.getConnection("FoPzlVote");
						Statement stmt = con.createStatement()) {
					stmt.executeUpdate("DELETE FROM fopzlvote_oldStreaks WHERE uuid = '" + uuid + "';");
				}
				catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(BukkitVote.getInstance());
		
		VoteRewards.rewardVotes(p, voteStreak, votesQueued);
		if (BukkitVote.debug) {
			Bukkit.getLogger().info("[FoPzlVote] Handled queued votes for player " + uuid + ", total votes " + totalVotes + ", vote streak " + voteStreak + ", votes queued " + votesQueued);
		}
		voteStreak += votesQueued;
		votesQueued = 0;

		dirty = true;
	}
	
	// This method MUST be called synchronously
	public void handleVote(String site) {
		totalVotes++;
		
		// Check for streak loss
		LocalDateTime time = LocalDateTime.now();
		if (Duration.between(lastVoted, time).compareTo(Duration.ofDays(streakResetTime)) > 0 && votesQueued > 0 && voteStreak > 0) {
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
		
		Player p = Bukkit.getPlayer(uuid);
		// If our votes are caught up (player is online), reward votes as normal
		if (votesQueued == 0 && oldStreaks.isEmpty() && p != null) {
			VoteRewards.rewardVotes(p, voteStreak++, 1);
		}
		// Otherwise just increase the streak (Waiting for login to kick in and reward all votes at once)
		else {
			votesQueued++;
		}
		dirty = true;

		if (BukkitVote.debug) {
			Bukkit.getLogger().info("[FoPzlVote] Handled vote for player " + uuid + ", total votes " + totalVotes + ", vote streak " + voteStreak + ", votes queued " + votesQueued);
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
	
	public void updateGlobalStats(Statement stmt) {
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM MLMC.fopzlvote_playerStats WHERE uuid = '" + uuid + "';");
			totalVotes = rs.getInt("totalVotes");
			lastVoted = rs.getObject("whenLastVoted", LocalDateTime.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
			dirty = false;
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
