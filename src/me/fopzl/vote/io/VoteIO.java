package me.fopzl.vote.io;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.Vote;
import me.fopzl.vote.VoteParty;
import me.neoblade298.neocore.NeoCore;
import me.neoblade298.neocore.io.IOComponent;
import me.neoblade298.neocore.scheduler.ScheduleInterval;
import me.neoblade298.neocore.scheduler.SchedulerAPI;

public class VoteIO implements IOComponent {
	
	public VoteIO() {
		NeoCore.registerIOComponent(Vote.getInstance(), this, "FoPzlVoteIO");
		
		loadVoteParty();
		
		SchedulerAPI.scheduleRepeating("FoPzlVote-Autosave-Queue", ScheduleInterval.FIFTEEN_MINUTES, new Runnable() {
			public void run() {
				new BukkitRunnable() {
					public void run() {
						saveQueue();
						saveVoteParty();
					}
				}.runTaskAsynchronously(Vote.getInstance());
			}
		});
	}
	
	@Override
	public void cleanup(Statement insert, Statement delete) {
		saveQueue();
		saveVoteParty();
	}

	@Override
	public void preloadPlayer(OfflinePlayer arg0, Statement arg1) {}

	@Override
	public void savePlayer(Player p, Statement insert, Statement delete) {
		autosavePlayer(p, insert, delete);
		VoteInfo.globalStats.remove(p.getUniqueId());
		VoteInfo.localStats.remove(p.getUniqueId());
	}
	
	@Override
	public void autosavePlayer(Player p, Statement insert, Statement delete) {
		UUID uuid = p.getUniqueId();
		if(!VoteInfo.globalStats.containsKey(uuid)) return;
		
		VoteStatsGlobal vs = VoteInfo.globalStats.get(uuid);
		if(!vs.needToSave) return;
		vs.needToSave = false;
		
		try {
			insert.addBatch("replace into fopzlvote_playerStats values ('" + uuid + "', " + vs.totalVotes + ", " + vs.voteStreak + ", '" + vs.lastVoted.toString() + "');");
			
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
		
		if(!loadedOfflinePlayers.contains(uuid)) {
			try {
				ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
				if(!rs.next()) return;
				
				VoteStatsGlobal vs = new VoteStatsGlobal(rs.getInt("totalVotes"), rs.getInt("voteStreak"), rs.getObject("whenLastVoted", LocalDateTime.class));
				
				rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
				while(rs.next()) {
					VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
					String voteSite = rs.getString("voteSite");
					int numVotes = rs.getInt("numVotes");
					
					Map<String, Integer> monthCounts = vs.monthlySiteCounts.getOrDefault(voteMonth, new HashMap<String, Integer>());
					monthCounts.put(voteSite, numVotes);
					vs.monthlySiteCounts.putIfAbsent(voteMonth, monthCounts);
				}
				
				VoteInfo.globalStats.put(uuid, vs);
			} catch (SQLException e) {
				Bukkit.getLogger().warning("Failed to load vote data for player " + p.getName());
				e.printStackTrace();
			}
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				Map<UUID, Map<String, Integer>> qr = VoteInfo.queuedRewards;
				if(qr.containsKey(uuid)) {
					Map<String, Integer> sites = qr.remove(uuid);
					int sum = 0;
					for(Entry<String, Integer> entry : sites.entrySet()) {
						// Calculate how many offline votes there was to properly reward the streaks
						sum += entry.getValue();
					}
					Vote.rewardVoteQueued(p, sum);
				}
			}
		}.runTask(Vote.getInstance());
	}
	
	public static VoteStatsGlobal tryLoadGlobalStats(OfflinePlayer p) {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		UUID uuid = p.getUniqueId();
		
		try {
			ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
			if(!rs.next()) return null;
			
			VoteStatsGlobal vs = new VoteStatsGlobal(rs.getInt("totalVotes"), rs.getInt("voteStreak"), rs.getObject("whenLastVoted", LocalDateTime.class));
			
			rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
				String voteSite = rs.getString("voteSite");
				int numVotes = rs.getInt("numVotes");
				
				Map<String, Integer> monthCounts = vs.monthlySiteCounts.getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.monthlySiteCounts.putIfAbsent(voteMonth, monthCounts);
			}
			
			VoteInfo.putGlobalStats(uuid, vs);
			
			return vs;
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for player " + p.getName());
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static VoteStatsLocal tryLoadLocalStats(OfflinePlayer p) {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		UUID uuid = p.getUniqueId();
		
		try {
			ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
			if(!rs.next()) return null;
			
			VoteStatsLocal vs = new VoteStatsLocal(rs.getInt("totalVotes"), rs.getInt("voteStreak"), 0, 0, rs.getObject("whenLastVoted", LocalDateTime.class));
			
			rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				
			}

			VoteInfo.putLocalStats(uuid, vs);
			
			return vs;
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for player " + p.getName());
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void saveVoteParty() {
		Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
		
		try {
			stmt.execute("delete from fopzlvote_voteParty;");
			stmt.execute("insert into fopzlvote_voteParty values (" + VoteParty.getPoints() + ");");
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
				
				VoteParty.setPoints(pts);
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
		
		for(Entry<UUID, Map<String, Integer>> entry : VoteInfo.queuedRewards.entrySet()) {
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
	
	public static void setCooldown(OfflinePlayer player, String voteSite) {
		try {
			Statement stmt = NeoCore.getStatement("FoPzlVoteIO");
			
			UUID uuid = player.getUniqueId();
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
	
	public static void addOfflinePlayer(UUID uuid) {
		loadedOfflinePlayers.add(uuid);
	}
}
