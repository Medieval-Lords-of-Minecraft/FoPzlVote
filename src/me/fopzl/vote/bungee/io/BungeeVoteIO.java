package me.fopzl.vote.bungee.io;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import me.fopzl.vote.bungee.BungeeVote;
import me.fopzl.vote.bungee.BungeeVoteParty;
import me.neoblade298.neocore.bungee.BungeeCore;

public class BungeeVoteIO {
	
	public static void load() {
		loadVoteParty();
		BungeeVote.inst().getProxy().getScheduler().schedule(BungeeVote.inst(), () -> {
			saveVoteParty();
		}, 15, TimeUnit.MINUTES);
	}
	
	public static void loadVoteParty() {
		try (Connection con = BungeeCore.getConnection("FoPzlVoteIO");
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("select points from fopzlvote_voteParty limit 1;");){
			if(rs.next()) {
				int pts = rs.getInt("points");
				
				BungeeVoteParty.setPoints(pts);
			}
			stmt.close();
		} catch (SQLException e) {
			BungeeVote.inst().getLogger().warning("Failed to load vote party points");
			e.printStackTrace();
		}
	}
	
	public static void saveVoteParty() {
		try (Connection con = BungeeCore.getConnection("FoPzlVoteIO");
				Statement stmt = con.createStatement();){
			stmt.execute("delete from fopzlvote_voteParty;");
			stmt.execute("insert into fopzlvote_voteParty values (" + BungeeVoteParty.getPoints() + ");");
			stmt.close();
		} catch (SQLException e) {
			BungeeVote.inst().getLogger().warning("Failed to save vote party points");
			e.printStackTrace();
		}
	}
}
