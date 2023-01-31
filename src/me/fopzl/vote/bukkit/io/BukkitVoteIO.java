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
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.io.IOComponent;

public class BukkitVoteIO implements IOComponent {
	public static HashMap<UUID, VoteStats> stats = new HashMap<UUID, VoteStats>();
	
	@Override
	public void cleanup(Statement insert, Statement delete) {}
	
	@Override
	public void autosave(Statement insert, Statement delete) {
		// Remove uuids that aren't online and haven't voted in the past 15-30 minutes
		ArrayList<UUID> canRemove = new ArrayList<UUID>();
		for (Entry<UUID, VoteStats> e : stats.entrySet()) {
			if (e.getValue().isDirty()) e.getValue().save(insert, delete);
			
			if (Bukkit.getPlayer(e.getKey()) == null &&
					Duration.between(e.getValue().getLastVoted(), LocalDateTime.now()).compareTo(Duration.of(15, ChronoUnit.MINUTES)) > 0) {
				canRemove.add(e.getKey());
			}
		}
		try {
			insert.executeBatch();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		for (UUID uuid : canRemove) {
			stats.remove(uuid);
		}
	}

	@Override
	public void preloadPlayer(OfflinePlayer arg0, Statement arg1) {}

	@Override
	// Handled by autosave
	public void savePlayer(Player p, Statement insert, Statement delete) {}
	
	@Override
	// handled by general autosave since player may not be online
	public void autosavePlayer(Player p, Statement insert, Statement delete) {}

	@Override
	public void loadPlayer(Player p, Statement stmt) {
		UUID uuid = p.getUniqueId();
		loadPlayer(uuid, stmt);
		new BukkitRunnable() {
			public void run() {
				if (stats.containsKey(uuid)) {
					stats.get(uuid).
				}
			}
		}.runTaskLater(BukkitVote.getInstance(), 100L);
	}
	
	private static void loadPlayer(UUID uuid, Statement stmt) {
		try {
			ResultSet rs1 = stmt.executeQuery("select * from fopzlvote_playerStats where uuid = '" + uuid + "';");
			if(!rs1.next()) return;

			ResultSet rs2 = stmt.executeQuery("select * from fopzlvote_playerQueue where uuid = '" + uuid + "' and server = '" + NeoCore.getInstanceKey() + "';");
			rs2.next();
			VoteStats vs = new VoteStats(uuid, rs1.getInt("totalVotes"), rs2.getInt("voteStreak"), rs2.getInt("votesQueued"), rs1.getObject("whenLastVoted", LocalDateTime.class));
			rs1.close();
			rs2.close();
			
			// Player history
			ResultSet rs = stmt.executeQuery("select * from fopzlvote_playerHist where uuid = '" + uuid + "';");
			while(rs.next()) {
				VoteMonth voteMonth = new VoteMonth(rs.getInt("year"), rs.getInt("month"));
				String voteSite = rs.getString("voteSite");
				int numVotes = rs.getInt("numVotes");
				
				Map<String, Integer> monthCounts = vs.getMonthlySiteCounts().getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.getMonthlySiteCounts().putIfAbsent(voteMonth, monthCounts);
			}
			
			// Old streaks
			rs = stmt.executeQuery("select * from fopzlvote_oldStreaks where uuid = '" + uuid + "';");
			while(rs.next()) {
				vs.addStreak(new OldVoteStreak(rs.getInt("voteStreak"), rs.getInt("votesQueued")));
			}
			
			rs.close();
			stats.put(uuid, vs);
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Failed to load vote data for player " + uuid);
			e.printStackTrace();
		}
	}
	
	public static VoteStats loadOrGetStats(UUID uuid) {
		if (stats.containsKey(uuid)) return stats.get(uuid);
		
		try (Connection con = NeoCore.getConnection("FoPzlVote");
				Statement stmt = con.createStatement()) {
			loadPlayer(uuid, stmt);
			return stats.get(uuid);
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
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
}
