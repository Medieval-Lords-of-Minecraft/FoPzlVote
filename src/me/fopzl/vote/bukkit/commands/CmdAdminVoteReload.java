package me.fopzl.vote.bukkit.commands;

import org.bukkit.command.CommandSender;

import me.fopzl.vote.bukkit.BukkitVote;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdAdminVoteReload extends Subcommand {

	public CmdAdminVoteReload(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("username", false), new Arg("site"));
		aliases = new String[] {"cd"};
	}

	@Override
	public void run(CommandSender s, String[] args) {
		BukkitVote.loadAllConfigs();
		Util.msg(s, "&7Reloaded config");
	}
}
