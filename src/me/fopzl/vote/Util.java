package me.fopzl.vote;

import org.bukkit.command.CommandSender;

import me.neoblade298.neocore.bungee.BungeeAPI;

public class Util {
	
	public static void sendMessageFormatted(CommandSender sender, String msg) {
		sender.sendMessage(msg.replace('&', '§'));
	}
}
