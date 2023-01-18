package me.fopzl.vote.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.fopzl.vote.SpigotVote;
import me.neoblade298.neocore.bungee.PluginMessageEvent;

public class ProxyListener implements Listener {
	SpigotVote main;
	public ProxyListener(SpigotVote main) {
		this.main = main;
	}
	@EventHandler
	public void onVoteNotify(PluginMessageEvent e) {
		if (!e.getChannel().equals("fopzlvote-vote")) return;
		
		Player p = Bukkit.getPlayer(UUID.fromString(e.getMessages().get(0)));
		
		if (p != null) {
			main.rewardVote(p);
		}
		else {
			
		}
	}
}
