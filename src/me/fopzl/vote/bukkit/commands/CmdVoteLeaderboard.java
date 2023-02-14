package me.fopzl.vote.bukkit.commands;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.bukkit.BukkitVote;
import me.fopzl.vote.bukkit.io.BukkitVoteIO;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteLeaderboard extends Subcommand {

	public CmdVoteLeaderboard(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		aliases = new String[] {"lb"};
	}
 
	@Override
	public void run(CommandSender s, String[] args) {
		new BukkitRunnable() {
			public void run() {
				List<Object[]> topVoters = BukkitVoteIO.getTopVoters(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
				int num = 1;
				String msg = "&eTop Monthly Voters:";
				for (Object[] entry : topVoters) {
					String username = Bukkit.getServer().getOfflinePlayer((UUID) entry[0]).getName();
					msg += "\n&6&l" + num++ + ". &e" + username + " &7- &f" + (int) entry[1];
				}

				Util.msg(s, msg);
			}
		}.runTaskAsynchronously(BukkitVote.getInstance());
	}
}
