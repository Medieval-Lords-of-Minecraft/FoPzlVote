package me.fopzl.vote;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class VoteInfo {
	public Map<UUID, VoteStats> playerStats;
	public Map<UUID, Map<String, Integer>> queuedRewards;
	
	private Vote main;
	
	public VoteInfo(Vote main) {
		this.main = main;
		
		playerStats = new HashMap<UUID, VoteStats>();
		queuedRewards = new HashMap<UUID, Map<String, Integer>>();
	}
	
	public VoteStats getStats(OfflinePlayer p) {
		UUID uuid = p.getUniqueId();
		if(playerStats.containsKey(uuid)) {
			return playerStats.get(uuid);
		} else {
			VoteStats vs = main.getVoteIO().tryLoadStats(p);
			if(vs == null) {
				vs = new VoteStats();
				playerStats.put(uuid, vs);
			}
			
			return vs;
		}
	}
}