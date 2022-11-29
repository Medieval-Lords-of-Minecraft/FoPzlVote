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

public class VoteParty {
	private Vote main;
	private VotePartyConfig cfg;
	
	private int points;
	
	public VoteParty(Vote main) {
		this.main = main;
		points = 0;
	}
	
	public void loadConfig(YamlConfiguration cfg) {
		this.cfg = new VotePartyConfig();
		this.cfg.pointsToStart = cfg.getInt("general.voteparty");
		
		this.cfg.countdownLength = cfg.getInt("general.countdown");
		this.cfg.countdownCommands = new HashMap<Integer, String>();
		ConfigurationSection countdownSec = cfg.getConfigurationSection("countdown"); 
		for(String key : countdownSec.getKeys(false)) {
			int countNum = Integer.parseInt(key);
			String cmd = countdownSec.getString(key);
			this.cfg.countdownCommands.put(countNum, cmd);
		}
		
		this.cfg.partyCommands = cfg.getStringList("general.voteparty-commands");
		
		this.cfg.notifyCommand = cfg.getString("notifications.interval.command");
		this.cfg.notifyInterval = cfg.getInt("notifications.interval.num");
		
		this.cfg.specificNotifies = new HashMap<Integer, String>();
		ConfigurationSection specificSec = cfg.getConfigurationSection("notifications.specific"); 
		for(String key : specificSec.getKeys(false)) {
			int pointNum = Integer.parseInt(key);
			String cmd = specificSec.getString(key);
			this.cfg.specificNotifies.put(pointNum, cmd);
		}
	}
	
	public void showStatus(Player p) {
		Util.sendMessageFormatted(p, "&4[&c&lMLMC&4] &e" + points + " / " + cfg.pointsToStart + " &7votes for a vote party to commence!");
	}
	
	public void addPoints(int pts) {
		points += pts;
		tick();
	}
	
	public void setPoints(int pts) {
		points = pts;
	}
	
	public int getPoints() {
		return points;
	}
	
	private void tick() {
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
	
	private void tryStartCountdown() {
		if(points >= cfg.pointsToStart) {
			points = 0;
			
			for(Entry<Integer, String> entry : cfg.countdownCommands.entrySet()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), entry.getValue());
				}}.runTaskLater(main, 20 * entry.getKey());
			}
			
			new BukkitRunnable() {
				@Override
				public void run() {
					startParty();
			}}.runTaskLater(main, 20 * cfg.countdownLength);
		}
	}
	
	private void startParty() {
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