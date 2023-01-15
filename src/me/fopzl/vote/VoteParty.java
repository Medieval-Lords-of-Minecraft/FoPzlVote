package me.fopzl.vote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.neoblade298.neocore.util.Util;

public class VoteParty {
	private static VotePartyConfig cfg;
	
	private static int points;
	
	public VoteParty(Vote main) {
		points = 0;
	}
	
	public void loadConfig(YamlConfiguration yml) {
		cfg = new VotePartyConfig();
		cfg.pointsToStart = yml.getInt("general.voteparty");
		
		cfg.countdownLength = yml.getInt("general.countdown");
		cfg.countdownCommands = new HashMap<Integer, String>();
		ConfigurationSection countdownSec = yml.getConfigurationSection("countdown"); 
		for(String key : countdownSec.getKeys(false)) {
			int countNum = Integer.parseInt(key);
			String cmd = countdownSec.getString(key);
			cfg.countdownCommands.put(countNum, cmd);
		}
		
		cfg.partyCommands = yml.getStringList("general.voteparty-commands");
		
		cfg.notifyCommand = yml.getString("notifications.interval.command");
		cfg.notifyInterval = yml.getInt("notifications.interval.num");
		
		cfg.specificNotifies = new HashMap<Integer, String>();
		ConfigurationSection specificSec = yml.getConfigurationSection("notifications.specific"); 
		for(String key : specificSec.getKeys(false)) {
			int pointNum = Integer.parseInt(key);
			String cmd = specificSec.getString(key);
			cfg.specificNotifies.put(pointNum, cmd);
		}
	}
	
	public static void showStatus(Player p) {
		Util.msg(p, "&e" + points + " / " + cfg.pointsToStart + " &7votes for a vote party to commence!");
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
		if(points % cfg.notifyInterval == 0) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cfg.notifyCommand.replace("%points%", points + "").replace("%votesremaining%", (cfg.pointsToStart - points) + ""));
		}
		
		for(int i : cfg.specificNotifies.keySet()) {
			if(points == i) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cfg.specificNotifies.get(i));
			}
		}
		
		tryStartCountdown();
	}
	
	private static void tryStartCountdown() {
		if(points >= cfg.pointsToStart) {
			points = 0;
			
			for(Entry<Integer, String> entry : cfg.countdownCommands.entrySet()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), entry.getValue());
				}}.runTaskLater(Vote.getInstance(), 20 * entry.getKey());
			}
			
			new BukkitRunnable() {
				@Override
				public void run() {
					startParty();
			}}.runTaskLater(Vote.getInstance(), 20 * cfg.countdownLength);
		}
	}
	
	private static void startParty() {
		for(String cmd : cfg.partyCommands) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
		}
	}
}

class VotePartyConfig {
	public int pointsToStart;
	public List<String> partyCommands; // ordered
	
	public int notifyInterval; // in points
	public String notifyCommand;
	
	public Map<Integer, String> specificNotifies;

	public int countdownLength; // in seconds
	public Map<Integer, String> countdownCommands; // key in seconds
}