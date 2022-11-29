package me.fopzl.vote;

import com.vexsoftware.votifier.model.VotifierEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VoteListener implements Listener {
	private Vote main;
	
	public VoteListener(Vote main) {
		this.main = main;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onVote(final VotifierEvent e) {
		com.vexsoftware.votifier.model.Vote vote = e.getVote();
		String site = vote.getServiceName();
		if(main.isValidSite(site) || site.equalsIgnoreCase("freevote")) {
			OfflinePlayer p = Bukkit.getServer().getOfflinePlayer(vote.getUsername());
			Util.broadcastFormatted("&4[&c&lMLMC&4] &e" + p.getName() + " &7just voted on &c" + site + "&7!");
			
			main.countVote((Player)p, site);
			
			if(p.isOnline()) {
				main.rewardVote((Player)p, site);
			} else {
				UUID uuid = p.getUniqueId();
				Map<String, Integer> pq = null;
				Map<UUID, Map<String, Integer>> qr = main.getVoteInfo().queuedRewards;
				
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
		}
	}
}
