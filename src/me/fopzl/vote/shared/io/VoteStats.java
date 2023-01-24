package me.fopzl.vote.shared.io;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoteStats {
	public static Map<UUID, VoteStatsGlobal> globalStats;
	public static Map<UUID, VoteStatsLocal> localStats;
	public static Map<UUID, Map<String, Integer>> queuedRewards;
	
	public VoteStats() {
		globalStats = new HashMap<UUID, VoteStatsGlobal>();
		queuedRewards = new HashMap<UUID, Map<String, Integer>>();
	}
	
	public static void putGlobalStats(UUID uuid, VoteStatsGlobal stats) {
		globalStats.put(uuid, stats);
	}
	
	public static void putLocalStats(UUID uuid, VoteStatsLocal stats) {
		localStats.put(uuid, stats);
	}
	
	public static VoteStatsGlobal getGlobalStats(UUID uuid) {
		if(globalStats.containsKey(uuid)) {
			return globalStats.get(uuid);
		} else {
			// Caches automatically
			return SharedVoteIO.loadGlobalStats(uuid);
		}
	}
	
	public static VoteStatsLocal getLocalStats(UUID uuid) {
		if(localStats.containsKey(uuid)) {
			return localStats.get(uuid);
		} else {
			/*
			VoteStatsLocal vs = VoteIO.loadLocalStats(p.getUniqueId());
			if(vs == null) {
				vs = new VoteStatsLocal();
				localStats.put(uuid, vs);
			}
			
			return vs;
			*/
			// TODO
			return null;
		}
	}
}