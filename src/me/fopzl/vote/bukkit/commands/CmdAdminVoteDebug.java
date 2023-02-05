package me.fopzl.vote.bukkit.commands;

import org.bukkit.command.CommandSender;

import me.fopzl.vote.bukkit.BukkitVote;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdAdminVoteDebug extends Subcommand {

	public CmdAdminVoteDebug(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSender s, String[] args) {
		BukkitVote.debug = !BukkitVote.debug;
		Util.msg(s, "&7Set debug to " + BukkitVote.debug);
	}
}
