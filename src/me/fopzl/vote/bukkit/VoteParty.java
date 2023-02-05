package me.fopzl.vote.bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class VoteParty {
	public static List<String> partyCommands; // ordered
	public static int countdownLength; // in seconds
	public static Map<Integer, String> countdownCommands; // key in seconds
	
	public void reload(YamlConfiguration yml) {
		countdownLength = yml.getInt("delay");
		countdownCommands = new HashMap<Integer, String>();
		ConfigurationSection countdownSec = yml.getConfigurationSection("countdown"); 
		if (countdownSec != null) {
			for(String key : countdownSec.getKeys(false)) {
				int countNum = Integer.parseInt(key);
				String cmd = countdownSec.getString(key);
				countdownCommands.put(countNum, cmd);
			}
		}
		
		partyCommands = yml.getStringList("commands");
	}
	
	public static void startCountdown() {
		for(Entry<Integer, String> entry : countdownCommands.entrySet()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), entry.getValue());
			}}.runTaskLater(BukkitVote.getInstance(), 20 * entry.getKey());
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				startParty();
		}}.runTaskLater(BukkitVote.getInstance(), 20 * countdownLength);
	}
	
	private static void startParty() {
		for(String cmd : partyCommands) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
		}
	}
}