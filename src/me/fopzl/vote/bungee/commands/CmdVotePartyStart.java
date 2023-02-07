package me.fopzl.vote.bungee.commands;

import me.fopzl.vote.bungee.BungeeVoteParty;
import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class CmdVotePartyStart extends Subcommand {

	public CmdVotePartyStart(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		color = ChatColor.DARK_RED;
	}

	@Override
	public void run(CommandSender s, String[] args) {
		BungeeVoteParty.triggerParties();
		Util.msg(s, "&7Started vote party");
	}
}
