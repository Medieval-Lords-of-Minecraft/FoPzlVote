package me.fopzl.vote.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.fopzl.vote.VoteParty;

public class VotePartyCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
		if(args.length < 1) return false;
		
		if(sender.hasPermission("mlvote.admin")) {
			switch(args[0]) {
				case "add":
					if(args.length < 2) return false;
					VoteParty.addPoints(Integer.parseInt(args[1]));
					return true;
				case "set":
					if(args.length < 2) return false;
					VoteParty.setPoints(Integer.parseInt(args[1]));
					return true;
			}
		}
		
		if (args[0].equalsIgnoreCase("status")) {
			VoteParty.showStatus((Player)sender);
			return true;
		}
		
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String lbl, String[] args) {
		if(args.length == 1) {
			List<String> options = new ArrayList<String>();
			options.add("status");
			if(sender.hasPermission("mlvote.admin")) {
				options.add("add");
				options.add("set");
			}
			return options;
		} else return null;
	}

}
