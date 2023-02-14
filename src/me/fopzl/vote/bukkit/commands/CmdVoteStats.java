package me.fopzl.vote.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.bukkit.BukkitVote;
import me.fopzl.vote.bukkit.io.BukkitVoteIO;
import me.fopzl.vote.bukkit.io.VoteStats;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteStats extends Subcommand {

	public CmdVoteStats(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player", false));
	}

	@Override
	public void run(CommandSender s, String[] args) {
		new BukkitRunnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				if(args.length == 1) {
					showStats(s, Bukkit.getOfflinePlayer(args[0]));
				} else {
					showStats(s, ((Player) s));
				}
			}
		}.runTaskAsynchronously(BukkitVote.getInstance());
	}

	private void showStats(CommandSender showTo, OfflinePlayer p) {
		VoteStats stats = BukkitVoteIO.loadOrGetStats(p.getUniqueId());

		String msg = "&eVote Stats for &6" + p.getName() + "&e:" + "\n &eAll time votes: &7"
				+ stats.getTotalVotes() + "\n &eVotes this month: &7" + stats.getVotesThisMonth()
				+ "\n &eCurrent Streak: &7" + stats.getStreak();

		Util.msg(showTo, msg);
	}
}
