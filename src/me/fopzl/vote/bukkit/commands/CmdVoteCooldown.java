package me.fopzl.vote.bukkit.commands;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.fopzl.vote.bukkit.BukkitVote;
import me.fopzl.vote.bukkit.VoteSiteInfo;
import me.fopzl.vote.bukkit.io.BukkitVoteIO;
import me.fopzl.vote.bukkit.io.VoteStats;
import me.fopzl.vote.shared.VoteUtil;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteCooldown extends Subcommand {

	public CmdVoteCooldown(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player", false));
		aliases = new String[] {"cd"};
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run(CommandSender s, String[] args) {
		new BukkitRunnable() {
			public void run() {
				if(args.length > 1) {
					OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
					showCooldowns(s, player);
				} else {
					showCooldowns(s, (Player) s);
				}
			}
		}.runTaskAsynchronously(BukkitVote.getInstance());
	}
	
	private void showCooldowns(CommandSender showTo, OfflinePlayer player) {
		VoteStats stats = BukkitVoteIO.loadOrGetStats(player.getUniqueId());
		if (stats == null) {
			Util.msg(showTo, "&cThat player doesn't exist!");
			return;
		}
		
		String msg = "&eVote Site Cooldowns for &6" + player.getName() + "&e:";
		LocalDateTime lastVoted = stats.getLastVoted();
		for (Entry<String, VoteSiteInfo> site : VoteUtil.getVoteSites().entrySet()) {
			Duration dur = site.getValue().cooldown.getCooldownRemaining(lastVoted);
			String cd = dur.isNegative() ? "&aReady!" : String.format("&c%02d:%02d:%02d", dur.toHours(), dur.toMinutesPart(), dur.toSecondsPart());
			
			msg += "\n &e" + site.getValue().nickname + ": " + cd;
		}

		Util.msg(showTo, msg);
	}
}
