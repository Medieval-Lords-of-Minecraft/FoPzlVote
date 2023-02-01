package me.fopzl.vote.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.fopzl.vote.bukkit.VoteRewards;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdAdminVoteReward extends Subcommand {

	public CmdAdminVoteReward(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player", false), new Arg("reward"));
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Player p = args.length == 2 ? Bukkit.getPlayer(args[0]) : (Player) s;
		
		if (VoteRewards.giveReward(p, args.length == 2 ? args[1] : args[0])) {
			Util.msg(p, "&7Unknown Reward: &e" + args[2]);
		}
		Util.msg(s, "Successfully gave vote reward to " + p.getName());
	}
}
