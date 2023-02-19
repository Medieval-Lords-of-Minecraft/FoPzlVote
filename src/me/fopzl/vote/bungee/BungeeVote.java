package me.fopzl.vote.bungee;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.alessiodp.lastloginapi.api.LastLogin;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;

import me.fopzl.vote.bukkit.VoteCooldown;
import me.fopzl.vote.bukkit.VoteSiteInfo;
import me.fopzl.vote.bungee.commands.*;
import me.fopzl.vote.bungee.io.BungeeVoteIO;
import me.fopzl.vote.shared.VoteUtil;
import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.bungee.commands.SubcommandManager;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class BungeeVote extends Plugin implements Listener
{
	private static BungeeVote inst;
	
    @Override
    public void onEnable() {
        inst = this;
        getProxy().getPluginManager().registerListener(this, this);
        BungeeVoteIO.load();
        
        initCommands();
        
        reload();
    }
    
    @Override
    public void onDisable() {
    	BungeeVoteIO.saveVoteParty();
    }
    
    private void initCommands() {
    	SubcommandManager mngr = new SubcommandManager("vp", null, ChatColor.RED, this);
    	mngr.register(new CmdVotePartyStatus("status", "Checks how many votes left till a vote party!", null, SubcommandRunner.BOTH));
    	mngr.register(new CmdVotePartySet("set", "Sets the vote party vote count", "fopzlvote.admin", SubcommandRunner.BOTH));
    	mngr.register(new CmdVotePartyStart("start", "Starts a vote party", "fopzlvote.admin", SubcommandRunner.BOTH));
    	mngr.registerCommandList("");
    }
	
	public void reload() {
		File mainCfg = new File(getDataFolder(), "config.yml");
		try {
			Configuration cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(mainCfg);
			VoteUtil.resetVoteSites();
			
			// Valid websites
			Configuration siteSec = cfg.getSection("websites");
			for (String siteNick : siteSec.getKeys()) {
				VoteSiteInfo vsi = new VoteSiteInfo();
				vsi.nickname = siteNick;
				
				Configuration subSec = siteSec.getSection(siteNick);
				vsi.serviceName = subSec.getString("serviceName");
				boolean cdType = subSec.getString("cooldownType").equalsIgnoreCase("fixed");
				int cdTime = subSec.getInt("cooldownTime");
				String timezone = subSec.getString("timezone");
				
				vsi.cooldown = new VoteCooldown(cdType, cdTime, timezone);

				VoteUtil.addVoteSite(vsi.serviceName, vsi);
			}
			
			Configuration sec = cfg.getSection("voteparty");
			BungeeVoteParty.load(sec);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public static void onVote(VotifierEvent e) {
		Vote vote = e.getVote();
		String site = vote.getServiceName();
		String user = vote.getUsername();
		final UUID uuid;
		try {
			uuid = VoteUtil.checkVote(user, site);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			inst.getLogger().warning("[FoPzlVote] Vote failed for username " + user + " on site " + site);
			return;
		}
		ProxiedPlayer p = inst.getProxy().getPlayer(uuid);
		String name = LastLogin.getApi().getPlayer(uuid).getName();
		
		Util.mutableBroadcast("votebc", "&e" + name + " &7just voted on &c" + site + "&7!");
		BungeeVoteParty.addPoints(1);
		
		// Update global stats, but after 5 seconds to give local stats chance to load them first
		inst.getProxy().getScheduler().schedule(inst, () -> {
			try (Connection con = BungeeCore.getConnection("FoPzlVote");
					Statement stmt = con.createStatement()) {
				int year = LocalDateTime.now().getYear();
				int month = LocalDateTime.now().getMonthValue();
				String whenLastVoted = LocalDateTime.now().toString();
				stmt.addBatch("update fopzlvote_playerStats set totalVotes = totalVotes + 1 where uuid = '" + uuid + "';");
				stmt.addBatch("update fopzlvote_playerStats set whenLastVoted = '" + LocalDateTime.now().toString() + "' where uuid = '" + uuid + "';");
				stmt.addBatch("update fopzlvote_playerHist set numVotes = numVotes + 1 where uuid = '" + uuid + "' and year = " + year + " and month = " + month +
						"and voteSite = '" + site + "';");
				if (p != null) {
					stmt.addBatch("update fopzlvote_playerQueue set votesQueued = votesQueued + 1 where uuid = '" + uuid +
							"' AND server != '" + p.getServer().getInfo().getName() + "';");
				}
				else {
					stmt.addBatch("update fopzlvote_playerQueue set votesQueued = votesQueued + 1 where uuid = '" + uuid + "';");
				}
				stmt.execute("replace into fopzlvote_siteCooldowns values ('" + uuid + "', '" + site + "', '" + whenLastVoted + "');");
				stmt.executeBatch();
			}
			catch (SQLException ex) {
				ex.printStackTrace();
			}
		}, 5, TimeUnit.SECONDS);
	}
	
	public static BungeeVote inst() {
		return inst;
	}
}
