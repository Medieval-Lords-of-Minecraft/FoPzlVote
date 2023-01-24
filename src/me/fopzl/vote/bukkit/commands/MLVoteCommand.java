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

import me.fopzl.vote.bukkit.SpigotVote;
import me.neoblade298.neocore.bukkit.util.BukkitUtil;

public class MLVoteCommand implements CommandExecutor, TabCompleter {
	private SpigotVote main;
	
	public MLVoteCommand(SpigotVote main) {
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

					if(!SpigotVote.giveReward(args[1], args[2])) {
						BukkitUtil.msg(sender, "&7Unknown Reward: &e" + args[2]);
					}
					return true;
				// mlvote vote [player] [website]
				case "vote":
					if(args.length < 3) return false;
					
					SpigotVote.cmdVote(args[1], args[2]);
					return true;
				case "setvotes":
					if(args.length < 3) return false;
					
					Player onlinePlayer = Bukkit.getPlayer(args[1]);
					int numVotes = Integer.parseInt(args[2]);
					if(onlinePlayer == null) {
						BukkitUtil.msg(sender, "&7Player &e" + args[1] + " &7offline.");
						return true;
					}
					SpigotVote.setTotalVotes(onlinePlayer, numVotes);
					return true;
				case "reload":
					main.loadAllConfigs();
					BukkitUtil.msg(sender, "&7Reloaded config");
					return true;
				case "debug":
					SpigotVote.debug = !SpigotVote.debug;
					BukkitUtil.msg(sender, "&7Set debug to " + SpigotVote.debug);
					return true;
			}
		}
		
		switch(args[0]) {
		case "stats":
			if(!(sender instanceof Player)) return false;
			if(args.length > 1) {
				Player onlinePlayer = Bukkit.getPlayer(args[1]);
				if(onlinePlayer == null) {
					BukkitUtil.msg(sender, "&7Player &e" + args[1] + " &7offline.");
					return true;
				}
				SpigotVote.showStats(sender, onlinePlayer);
			} else {
				SpigotVote.showStats(sender, (Player)sender);
			}
			return true;
		case "cooldown":
			if(!(sender instanceof Player)) return false;
			if(args.length > 1) {
				OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
				SpigotVote.showCooldowns(sender, player);
			} else {
				SpigotVote.showCooldowns(sender, (Player)sender);
			}
			return true;
		case "leaderboard":
			SpigotVote.showLeaderboard(sender);
			return true;
		}
		
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String lbl, String[] args) {
		if(args.length == 1) {
			List<String> options = new ArrayList<String>();
			options.add("stats");
			options.add("cooldown");
			options.add("leaderboard");
			if(sender.hasPermission("mlvote.admin")) {
				options.add("reward");
				options.add("vote");
				options.add("setvotes");
				options.add("reload");
				options.add("debug");
			}
			return options;
		} else return null;
	}
	
	

}