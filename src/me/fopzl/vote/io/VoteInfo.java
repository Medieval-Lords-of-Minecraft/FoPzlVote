package me.fopzl.vote.io;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

public class VoteInfo {
	static Map<UUID, VoteStatsGlobal> globalStats;
	static Map<UUID, VoteStatsLocal> localStats;
	static Map<UUID, Map<String, Integer>> queuedRewards;
	
	public VoteInfo() {
		globalStats = new HashMap<UUID, VoteStatsGlobal>();
		queuedRewards = new HashMap<UUID, Map<String, Integer>>();
	}
	
	public static void putGlobalStats(UUID uuid, VoteStatsGlobal stats) {
		globalStats.put(uuid, stats);
	}
	
	public static void putLocalStats(UUID uuid, VoteStatsLocal stats) {
		localStats.put(uuid, stats);
	}
	
	public static VoteStatsGlobal getGlobalStats(OfflinePlayer p) {
		UUID uuid = p.getUniqueId();
		if(globalStats.containsKey(uuid)) {
			return globalStats.get(uuid);
		} else {
			VoteStatsGlobal vs = VoteIO.tryLoadGlobalStats(p);
			if(vs == null) {
				vs = new VoteStatsGlobal();
				globalStats.put(uuid, vs);
			}
			
			return vs;
		}
	}
	
	public static VoteStatsLocal getLocalStats(OfflinePlayer p) {
		UUID uuid = p.getUniqueId();
		if(localStats.containsKey(uuid)) {
			return localStats.get(uuid);
		} else {
			VoteStatsLocal vs = VoteIO.tryLoadLocalStats(p);
			if(vs == null) {
				vs = new VoteStatsLocal();
				localStats.put(uuid, vs);
			}
			
			return vs;
		}
	}
}