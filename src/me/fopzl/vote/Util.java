package me.fopzl.vote;

import org.bukkit.command.CommandSender;

import me.neoblade298.neocore.bungee.BungeeAPI;

public class Util {
	public static void broadcastFormatted(String msg) {
		// for debug
		//Bukkit.getLogger().info("[MLVote]" + msg);
		
		// for use on non-bungee servers
		// Bukkit.broadcastMessage(msg.replace('&', '§'));
		
		// for use on bungee servers
		BungeeAPI.broadcast(msg.replace('&', '§'));
	}
	
	public static void sendMessageFormatted(CommandSender sender, String msg) {
		sender.sendMessage(msg.replace('&', '§'));
	}
}
