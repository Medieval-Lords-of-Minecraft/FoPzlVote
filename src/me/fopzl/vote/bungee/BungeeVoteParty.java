package me.fopzl.vote.bungee;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.SpigotVote;
import me.neoblade298.bungeecore.BungeeCore;
import me.neoblade298.neocore.util.Util;

public class BungeeVoteParty {
	private static int pointsToStart, points, notifyInterval;
	private static String notifyCommand;
	private static HashMap<Integer, String> specificNotifies = new HashMap<Integer, String>();
	private static HashMap<String, Integer> servers = new HashMap<String, Integer>();
	public BungeeVoteParty(ConfigurationSection sec) {
		pointsToStart = sec.getInt("points-to-start", 500);
		notifyCommand = sec.getString("notifications.interval.command");
		notifyInterval = sec.getInt("notifications.interval.num");
		
		ConfigurationSection specificSec = sec.getConfigurationSection("notifications.specific"); 
		for(String key : specificSec.getKeys(false)) {
			int pointNum = Integer.parseInt(key);
			String cmd = specificSec.getString(key);
			specificNotifies.put(pointNum, cmd);
		}
		
		ConfigurationSection serverSec = sec.getConfigurationSection("servers");
		for (String key : serverSec.getKeys(false)) {
			servers.put(key, serverSec.getInt(key));
		}
	}
	
	public static void showStatus(Player p) {
		Util.msg(p, "&e" + points + " / " + pointsToStart + " &7votes for a vote party to commence!");
	}
	
	public static void addPoints(int pts) {
		points += pts;
		tick();
	}
	
	public static void setPoints(int pts) {
		points = pts;
	}
	
	public static int getPoints() {
		return points;
	}
	
	private static void tick() {
		if(points % notifyInterval == 0) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), notifyCommand.replace("%points%", points + "").replace("%votesremaining%", (pointsToStart - points) + ""));
		}
		
		for(int i : specificNotifies.keySet()) {
			if(points == i) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), specificNotifies.get(i));
			}
		}
		
		if (points >= pointsToStart) {
			triggerParties();
		}
	}
	
	private static void triggerParties() {
		points = 0;
		for (Entry<String, Integer> e : servers.entrySet()) {
			if (e.getValue() == 0) {
				BungeeCore.sendPluginMessage(new String[] {e.getKey()}, new String[] {"fopzlvote-startparty"});
			}
			else {
				BungeeVote.inst().getProxy().getScheduler().schedule(BungeeVote.inst(), () -> {
					BungeeCore.sendPluginMessage(new String[] {e.getKey()}, new String[] {"fopzlvote-startparty"});
				}, e.getValue(), TimeUnit.MINUTES);
			}
		}
	}
}
