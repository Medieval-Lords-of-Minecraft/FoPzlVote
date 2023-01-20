package me.fopzl.vote.listeners;

import me.fopzl.vote.SpigotVote;
import me.neoblade298.neocore.bungee.BungeeAPI;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {
	@SuppressWarnings("deprecation")
	@EventHandler
	public static void onVote(final VotifierEvent e) {
		com.vexsoftware.votifier.model.Vote vote = e.getVote();
		String site = vote.getServiceName();
		if(SpigotVote.isValidSite(site) || site.equalsIgnoreCase("freevote")) {
			OfflinePlayer p = Bukkit.getServer().getOfflinePlayer(vote.getUsername());
			BungeeAPI.broadcast("&4[&c&lMLMC&4] &e" + p.getName() + " &7just voted on &c" + site + "&7!");
			
			BungeeAPI.sendPluginMessage("fopzlvote-vote", new String[] { p.getUniqueId().toString() });
			
			/*
			if(p.isOnline()) {
				main.rewardVote(p.getPlayer());
			} else {
				UUID uuid = p.getUniqueId();
				VoteIO.addOfflinePlayer(uuid);
				Map<String, Integer> pq = null;
				Map<UUID, Map<String, Integer>> qr = VoteInfo.queuedRewards;
				
				if(!qr.containsKey(uuid)) {
					pq = new HashMap<String, Integer>();
					qr.put(uuid, pq);
				} else {
					pq = qr.get(uuid);
				}
				pq.put(site, pq.getOrDefault(site, 0) + 1);
			}
			
			main.incVoteParty();
			main.setCooldown(p, site);
			*/
		}
	}
}
