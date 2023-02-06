package me.fopzl.vote.bungee.commands;

import me.fopzl.vote.bungee.BungeeVoteParty;
import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import net.md_5.bungee.api.CommandSender;

public class CmdVotePartyStatus extends Subcommand {

	public CmdVotePartyStatus(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Util.msg(s, "&7Votes to start vote party: &c" + BungeeVoteParty.getPoints() + " &7/ &c" + BungeeVoteParty.getPointsToStart());
	}
}
