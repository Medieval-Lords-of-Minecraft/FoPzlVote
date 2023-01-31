package me.fopzl.vote.bukkit.commands;

import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.messaging.MessagingManager;

public class CmdVote extends Subcommand {

	public CmdVote(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSender s, String[] args) {
		MessagingManager.sendMessage(s, s, "vote");
	}
}
