package me.fopzl.vote.bukkit.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.fopzl.vote.bukkit.BukkitVote;
import me.neoblade298.neocore.bukkit.util.Util;

public class MLVoteCommand implements CommandExecutor, TabCompleter {
	private BukkitVote main;
	
	public MLVoteCommand(BukkitVote main) {
		this.main = main;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
		if(args.length < 1) return false;
		
		if(sender.hasPermission("mlvote.admin")) {
			switch(args[0]) {
				// mlvote reward [player] [rewardname]
				case "reward":
					if(args.length < 3) return false;

					if(!BukkitVote.giveReward(args[1], args[2])) {
						Util.msg(sender, "&7Unknown Reward: &e" + args[2]);
					}
					return true;
				// mlvote vote [player] [website]
				case "vote":
					if(args.length < 3) return false;
					
					BukkitVote.cmdVote(args[1], args[2]);
					return true;
				case "setvotes":
					if(args.length < 3) return false;
					
					Player onlinePlayer = Bukkit.getPlayer(args[1]);
					int numVotes = Integer.parseInt(args[2]);
					if(onlinePlayer == null) {
						Util.msg(sender, "&7Player &e" + args[1] + " &7offline.");
						return true;
					}
					BukkitVote.setTotalVotes(onlinePlayer, numVotes);
					return true;
				case "reload":
					main.loadAllConfigs();
					Util.msg(sender, "&7Reloaded config");
					return true;
				case "debug":
					BukkitVote.debug = !BukkitVote.debug;
					Util.msg(sender, "&7Set debug to " + BukkitVote.debug);
					return true;
			}
		}
		
	}
}
