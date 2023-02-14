package me.fopzl.vote.bukkit.commands;

import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.bukkit.bungee.BungeeAPI;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdAdminVoteVote extends Subcommand {

	public CmdAdminVoteVote(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("username", false), new Arg("site"));
		aliases = new String[] {"cd"};
	}

	@Override
	public void run(CommandSender s, String[] args) {
		String name = args.length == 2 ? args[0] : s.getName();
		BungeeAPI.sendBungeeCommand("ptestvote " + name + " " + (args.length == 2 ? args[1] : args[0]));
	}
}
