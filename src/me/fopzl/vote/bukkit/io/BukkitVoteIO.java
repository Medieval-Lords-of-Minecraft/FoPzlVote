package me.fopzl.vote.bukkit.io;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.bukkit.BukkitVote;
import me.fopzl.vote.shared.io.VoteMonth;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.io.IOComponent;

public class BukkitVoteIO implements IOComponent {
	public static HashMap<UUID, VoteStats> stats = new HashMap<UUID, VoteStats>();
	
	@Override
	public void cleanup(Statement insert, Statement delete) {
		// saveQueue();
	}
	
	@Override
	public void autosave(Statement insert, Statement delete) {
		// saveQueue();
		
		// Remove uuids that aren't online and haven't voted in the past 15-30 minutes
		ArrayList<UUID> canRemove = new ArrayList<UUID>();
		for (Entry<UUID, VoteStats> e : stats.entrySet()) {
			if (Bukkit.getPlayer(e.getKey()) == null &&
					Duration.between(e.getValue().getLastVoted(), LocalDateTime.now()).compareTo(Duration.of(15, ChronoUnit.MINUTES)) > 0) {
				canRemove.add(e.getKey());
			}
		}
		
		for (UUID uuid : canRemove) {
			stats.remove(uuid).save(insert, delete);
		}
	}

	@Override
	public void preloadPlayer(OfflinePlayer arg0, Statement arg1) {}

	@Override
	public void savePlayer(Player p, Statement insert, Statement delete) {
		autosavePlayer(p, insert, delete);
		stats.remove(p.getUniqueId());
	}
	
	@Override
	public void autosavePlayer(Player p, Statement insert, Statement delete) {
		UUID uuid = p.getUniqueId();
		if(!stats.containsKey(uuid)) return;
		
		VoteStats vs = stats.get(uuid);
		if(!vs.isDirty()) return;
		
		try {
			// insert.addBatch("replace into fopzlvote_playerStats values ('" + uuid + "', " + vs.totalVotes + ", " + vs.voteStreak + ", '" + vs.lastVoted.toString() + "');");
			
			int year = LocalDateTime.now().getYear();
			int month = LocalDateTime.now().getMonthValue();
			VoteMonth now = new VoteMonth(year, month);
			if(month == 1) {
				year--;
				month = 12;
			}
			VoteMonth prev = new VoteMonth(year, month);
			
			// only save this month and the last
			Map<String, Integer> currCounts = vs.getMonthlySiteCounts().get(now);
			if(currCounts != null) {
				for(Entry<String, Integer> entry : currCounts.entrySet()) {
					insert.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
				}
			}
			
			Map<String, Integer> prevCounts = vs.getMonthlySiteCounts().get(prev);
			if(prevCounts != null) {
				for(Entry<String, Integer> entry : prevCounts.entrySet()) {
					insert.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
				}
			}
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to save vote data for player " + p.getName());
			e.printStackTrace();
		}
	}

	@Override
	public void loadPlayer(Player p, Statement stmt) {
		UUID uuid = p.getUniqueId();
		
		try {
			ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
			if(!rs.next()) return;
			
			VoteStats vs = new VoteStats(uuid, rs.getInt("totalVotes"), rs.getInt("voteStreak"), rs.getObject("whenLastVoted", LocalDateTime.class));
			
			rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
				String voteSite = rs.getString("voteSite");
				int numVotes = rs.getInt("numVotes");
				
				Map<String, Integer> monthCounts = vs.getMonthlySiteCounts().getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.getMonthlySiteCounts().putIfAbsent(voteMonth, monthCounts);
			}
			
			stats.put(uuid, vs);
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for player " + p.getName());
			e.printStackTrace();
		}
		
		// TODO
		/*
		new BukkitRunnable() {
			@Override
			public void run() {
				Map<UUID, Map<String, Integer>> qr = VoteStats.queuedRewards;
				if(qr.containsKey(uuid)) {
					Map<String, Integer> sites = qr.remove(uuid);
					int sum = 0;
					for(Entry<String, Integer> entry : sites.entrySet()) {
						// Calculate how many offline votes there was to properly reward the streaks
						sum += entry.getValue();
					}
					BukkitVote.rewardVoteQueued(p, sum);
				}
			}
		}.runTask(BukkitVote.getInstance());*/
	}
	
	// TODO
	/*
	public static void saveQueue() {
		try (Connection con = NeoCore.getConnection("FoPzlVoteIO")) {
			try (Statement stmt = con.createStatement()) {
				stmt.execute("delete from fopzlvote_voteQueue");
			} catch (SQLException e) {
				Bukkit.getLogger().warning("Failed to clear queue data");
				e.printStackTrace();
			}
		
			try (Statement stmt = con.createStatement()) {
				for(Entry<UUID, Map<String, Integer>> entry : VoteStats.queuedRewards.entrySet()) {
					UUID uuid = entry.getKey();
					try {
						for(Entry<String, Integer> subEntry : entry.getValue().entrySet()) {
							stmt.addBatch("replace into fopzlvote_voteQueue values ('" + uuid + "', '" + subEntry.getKey() + "', " + subEntry.getValue() + ");");
						}
					} catch (SQLException e) {
						Bukkit.getLogger().warning("Failed to save queue data for uuid " + uuid);
						e.printStackTrace();
					}
				}
				stmt.executeBatch();
				stmt.close();
			} catch (SQLException e) {
				Bukkit.getLogger().warning("Failed to save queue data batch");
				e.printStackTrace();
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	*/
	
	// month is 1-indexed
	// returned list items are indexed as: [0] - uuid (as UUID), [1] = # of votes (as int)
	public static List<Object[]> getTopVoters(int year, int month, int numVoters) {
		List<Object[]> topVoters = new ArrayList<Object[]>();

		try (Connection con = NeoCore.getConnection("FoPzlVoteIO");
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("select uuid, sum(numVotes) sumVotes from fopzlvote_playerHist where year = "
				+ year + " and month = " + month + " group by uuid order by sumVotes desc limit " + numVoters + ";");) {
			
			while(rs.next()) {
				UUID uuid = UUID.fromString(rs.getString("uuid"));
				int sumVotes = rs.getInt("sumVotes");
				
				topVoters.add(new Object[] { uuid, sumVotes });
			}
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to get vote leaderboard from sql");
			e.printStackTrace();
		}
		
		return topVoters;
	}
	
	public static void setCooldown(Statement stmt, UUID uuid, String voteSite) {
		try {
			String whenLastVoted = LocalDateTime.now().toString();
			
			stmt.execute("replace into fopzlvote_siteCooldowns values ('" + uuid + "', '" + voteSite + "', '" + whenLastVoted + "');");
			
			stmt.close();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to save voteSite cooldown");
			e.printStackTrace();
		}
	}
	
	public static LocalDateTime getCooldown(OfflinePlayer player, String voteSite) {
		try (Connection con = NeoCore.getConnection("FoPzlVoteIO");
				Statement stmt = con.createStatement();) {
			
			UUID uuid = player.getUniqueId();
			
			ResultSet rs = stmt.executeQuery("select whenLastVoted from fopzlvote_siteCooldowns where uuid = '" + uuid + "' and voteSite = '" + voteSite + "';");
			if(rs.next()) {
				return rs.getTimestamp("whenLastVoted").toLocalDateTime();
			}
			
			stmt.close();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load voteSite cooldown");
			e.printStackTrace();
		}
		
		return LocalDateTime.of(0, 1, 1, 0, 0);
	}

	private static VoteStats loadGlobalStats(UUID uuid) {
		try (Connection con = NeoCore.getConnection("FoPzlVoteIO");
				Statement stmt = con.createStatement();) {
			ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
			if(!rs.next()) {
				VoteStats stats = new VoteStats(uuid);
				return stats;
			}
			
			VoteStats vs = new VoteStats(uuid, rs.getInt("totalVotes"), rs.getInt("voteStreak"), rs.getObject("whenLastVoted", LocalDateTime.class));
			
			rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
				String voteSite = rs.getString("voteSite");
				int numVotes = rs.getInt("numVotes");
				
				Map<String, Integer> monthCounts = vs.getMonthlySiteCounts().getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.getMonthlySiteCounts().putIfAbsent(voteMonth, monthCounts);
			}
			
			stats.put(uuid, vs);
			
			return vs;
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for uuid " + uuid);
			e.printStackTrace();
		}

		VoteStats vs = new VoteStats(uuid);
		stats.put(uuid, vs);
		return vs;
	}
}
