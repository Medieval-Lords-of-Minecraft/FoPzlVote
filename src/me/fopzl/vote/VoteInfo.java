package me.fopzl.vote;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

public class VoteInfo {
	public Map<UUID, VoteStatsGlobal> globalStats;
	public Map<UUID, VoteStatsLocal> localStats;
	public Map<UUID, Map<String, Integer>> queuedRewards;
	
	private Vote main;
	
	public VoteInfo(Vote main) {
		this.main = main;
		
		globalStats = new HashMap<UUID, VoteStatsGlobal>();
		queuedRewards = new HashMap<UUID, Map<String, Integer>>();
	}
	
	public VoteStatsGlobal getGlobalStats(OfflinePlayer p) {
		UUID uuid = p.getUniqueId();
		if(globalStats.containsKey(uuid)) {
			return globalStats.get(uuid);
		} else {
			VoteStatsGlobal vs = main.getVoteIO().tryLoadStats(p);
			if(vs == null) {
				vs = new VoteStatsGlobal();
				globalStats.put(uuid, vs);
			}
			
			return vs;
		}
	}
	
	public VoteStatsLocal getLocalStats(OfflinePlayer p) {
		UUID uuid = p.getUniqueId();
		if(localStats.containsKey(uuid)) {
			return localStats.get(uuid);
		} else {
			VoteStatsGlobal vs = main.getVoteIO().tryLoadLocalStats(p);
			if(vs == null) {
				vs = new VoteStatsGlobal();
				localStats.put(uuid, vs);
			}
			
			return vs;
		}
	}
}