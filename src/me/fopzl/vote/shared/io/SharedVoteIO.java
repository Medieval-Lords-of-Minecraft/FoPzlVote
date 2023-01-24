package me.fopzl.vote.shared.io;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.neoblade298.neocore.bukkit.NeoCore;

public class SharedVoteIO {
	
	// Should only be accessed by VoteStats
	protected static VoteStatsGlobal loadGlobalStats(UUID uuid) {
		try (Connection con = NeoCore.getConnection("FoPzlVoteIO");
				Statement stmt = con.createStatement();){
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
				
				Map<String, Integer> monthCounts = vs.getMonthlySiteCounts().getOrDefault(voteMonth, new HashMap<String, Integer>());
				monthCounts.put(voteSite, numVotes);
				vs.getMonthlySiteCounts().putIfAbsent(voteMonth, monthCounts);
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
}
