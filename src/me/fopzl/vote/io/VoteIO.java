package me.fopzl.vote.io;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.SpigotVote;
import me.fopzl.vote.bungee.BungeeVote;
import me.fopzl.vote.bungee.BungeeVoteParty;
import me.neoblade298.neocore.NeoCore;
import me.neoblade298.neocore.io.IOComponent;
import me.neoblade298.neocore.scheduler.ScheduleInterval;
import me.neoblade298.neocore.scheduler.SchedulerAPI;

public class VoteIO implements IOComponent {
	
	public VoteIO(boolean bungee) {
		if (bungee) {
			loadVoteParty();
			BungeeVote.inst().getProxy().getScheduler().schedule(BungeeVote.inst(), () -> {
				saveVoteParty();
			}, 15, TimeUnit.MINUTES);
		}
		else {
			NeoCore.registerIOComponent(SpigotVote.getInstance(), this, "FoPzlVoteIO");
			
			SchedulerAPI.scheduleRepeating("FoPzlVote-Autosave-Queue", ScheduleInterval.FIFTEEN_MINUTES, new Runnable() {
				public void run() {
					new BukkitRunnable() {
						public void run() {
							saveQueue();
						}
					}.runTaskAsynchronously(SpigotVote.getInstance());
				}
			});
		}
	}
	
	@Override
	public void cleanup(Statement insert, Statement delete) {
		saveQueue();
	}

	@Override
	public void preloadPlayer(OfflinePlayer arg0, Statement arg1) {}

	@Override
	public void savePlayer(Player p, Statement insert, Statement delete) {
		autosavePlayer(p, insert, delete);
		VoteStats.globalStats.remove(p.getUniqueId());
		VoteStats.localStats.remove(p.getUniqueId());
	}
	
	@Override
	public void autosavePlayer(Player p, Statement insert, Statement delete) {
		UUID uuid = p.getUniqueId();
		if(!VoteStats.globalStats.containsKey(uuid)) return;
		
		VoteStatsGlobal vs = VoteStats.globalStats.get(uuid);
		if(!vs.canRemove) return;
		vs.canRemove = false;
		
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
			Map<String, Integer> currCounts = vs.monthlySiteCounts.get(now);
			if(currCounts != null) {
				for(Entry<String, Integer> entry : currCounts.entrySet()) {
					insert.addBatch("replace into fopzlvote_playerHist values ('" + uuid + "', " + year + ", " + month + ", '" + entry.getKey() + "', " + entry.getValue() + ");");
				}
			}
			
			Map<String, Integer> prevCounts = vs.monthlySiteCounts.get(prev);
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
			
			VoteStatsGlobal vs = new VoteStatsGlobal(uuid, rs.getInt("totalVotes"), rs.getInt("voteStreak"), rs.getObject("whenLastVoted", LocalDateTime.class));
			
			rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
				String voteSite = rs.getString("voteSite");
				int numVotes = rs.getInt("numVotes");
				
				Map<String, Integer> monthCounts = vs.monthlySiteCounts.getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.monthlySiteCounts.putIfAbsent(voteMonth, monthCounts);
			}
			
			VoteStats.globalStats.put(uuid, vs);
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for player " + p.getName());
			e.printStackTrace();
		}
		
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
					SpigotVote.rewardVoteQueued(p, sum);
				}
			}
		}.runTask(SpigotVote.getInstance());
	}
	
	// Should only be accessed by VoteStats
	protected static VoteStatsGlobal loadGlobalStats(UUID uuid) {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		
		try {
			ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
			if(!rs.next()) {
				VoteStatsGlobal stats = new VoteStatsGlobal(uuid);
				VoteStats.putGlobalStats(uuid, stats);
				return stats;
			}
			
			VoteStatsGlobal vs = new VoteStatsGlobal(uuid, rs.getInt("totalVotes"), rs.getInt("voteStreak"), rs.getObject("whenLastVoted", LocalDateTime.class));
			
			rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
				String voteSite = rs.getString("voteSite");
				int numVotes = rs.getInt("numVotes");
				
				Map<String, Integer> monthCounts = vs.monthlySiteCounts.getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.monthlySiteCounts.putIfAbsent(voteMonth, monthCounts);
			}
			
			VoteStats.putGlobalStats(uuid, vs);
			
			return vs;
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for uuid " + uuid);
			e.printStackTrace();
		}

		VoteStatsGlobal stats = new VoteStatsGlobal(uuid);
		VoteStats.putGlobalStats(uuid, stats);
		return stats;
	}
	
	public static void saveVoteParty() {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		
		try {
			stmt.execute("delete from fopzlvote_voteParty;");
			stmt.execute("insert into fopzlvote_voteParty values (" + BungeeVoteParty.getPoints() + ");");
			stmt.close();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to save vote party points");
			e.printStackTrace();
		}
	}
	
	public static void loadVoteParty() {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		
		ResultSet rs;
		try {
			rs = stmt.executeQuery("select points from fopzlvote_voteParty limit 1;");
			if(rs.next()) {
				int pts = rs.getInt("points");
				
				BungeeVoteParty.setPoints(pts);
			}
			stmt.close();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote party points");
			e.printStackTrace();
		}
	}
	
	public static void saveQueue() {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		
		try {
			stmt.execute("delete from fopzlvote_voteQueue");
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to clear queue data");
			e.printStackTrace();
		}
		
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
		
		try {
			stmt.executeBatch();
			stmt.close();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to save queue data batch");
			e.printStackTrace();
		}
	}
	
	// month is 1-indexed
	// returned list items are indexed as: [0] - uuid (as UUID), [1] = # of votes (as int)
	public static List<Object[]> getTopVoters(int year, int month, int numVoters) {
		List<Object[]> topVoters = new ArrayList<Object[]>();
		
		try {
			Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
			
			ResultSet rs = stmt.executeQuery("select uuid, sum(numVotes) sumVotes from fopzlvote_playerHist where year = " + year + " and month = " + month + " group by uuid order by sumVotes desc limit " + numVoters + ";");
			while(rs.next()) {
				UUID uuid = UUID.fromString(rs.getString("uuid"));
				int sumVotes = rs.getInt("sumVotes");
				
				topVoters.add(new Object[] { uuid, sumVotes });
			}
			
			stmt.close();
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
		try {
			Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
			
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
}
