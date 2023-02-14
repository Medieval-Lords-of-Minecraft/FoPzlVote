package me.fopzl.vote.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.fopzl.vote.bukkit.io.BukkitVoteIO;
import me.fopzl.vote.bukkit.io.VoteStats;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdAdminVoteSetstreak extends Subcommand {

	public CmdAdminVoteSetstreak(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player", false), new Arg("streak"));
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Player p = args.length == 2 ? Bukkit.getPlayer(args[0]) : (Player) s;
		if (p == null) {
			Util.msg(s, "&cThat player isn't online!");
			return;
		}
		
		VoteStats stats = BukkitVoteIO.loadOrGetStats(p.getUniqueId());
		stats.setStreak(Integer.parseInt(args.length == 2 ? args[1] : args[0]));
		Util.msg(s, "Successfully set streak for " + p.getName());
	}
}
