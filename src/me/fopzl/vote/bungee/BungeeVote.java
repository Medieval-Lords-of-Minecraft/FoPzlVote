package me.fopzl.vote.bungee;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;

import me.fopzl.vote.bukkit.VoteCooldown;
import me.fopzl.vote.bukkit.VoteSiteInfo;
import me.fopzl.vote.bukkit.io.VoteMonth;
import me.fopzl.vote.bungee.io.BungeeVoteIO;
import me.fopzl.vote.shared.VoteUtil;
import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.shared.util.SharedUtil;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeVote extends Plugin implements Listener
{
	private static BungeeVote inst;
	
    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        BungeeVoteIO.load();
        inst = this;
        
        reload();
    }
	
	public void reload() {
		File mainCfg = new File(getDataFolder(), "config.yml");
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(mainCfg);
		
		// Valid websites
		ConfigurationSection siteSec = cfg.getConfigurationSection("websites");
		for (String siteNick : siteSec.getKeys(false)) {
			VoteSiteInfo vsi = new VoteSiteInfo();
			vsi.nickname = siteNick;
			
			ConfigurationSection subSec = siteSec.getConfigurationSection(siteNick);
			vsi.serviceName = subSec.getString("serviceName");
			boolean cdType = subSec.getString("cooldownType").equalsIgnoreCase("fixed");
			int cdTime = subSec.getInt("cooldownTime");
			String timezone = subSec.getString("timezone");
			
			vsi.cooldown = new VoteCooldown(cdType, cdTime, timezone);

			VoteUtil.addVoteSite(vsi.serviceName, vsi);
		}
		
		ConfigurationSection sec = cfg.getConfigurationSection("voteparty");
		new BungeeVoteParty(sec);
	}

	@EventHandler
	public static void onVote(VotifierEvent e) {
		Vote vote = e.getVote();
		String site = vote.getServiceName();
		String user = vote.getUsername();
		UUID uuid = VoteUtil.checkVote(user, site);
		if (uuid == null) return;
		SharedUtil.broadcast("&e" + user + " &7just voted on &c" + site + "&7!");
		
		BungeeVoteParty.addPoints(1);
		
		// Update global stats, but after 5 seconds to give local stats chance to load them first
		inst.getProxy().getScheduler().schedule(inst, () -> {
			try (Connection con = BungeeCore.getConnection("FoPzlVote");
					Statement stmt = con.createStatement()) {
				int year = LocalDateTime.now().getYear();
				int month = LocalDateTime.now().getMonthValue();
				String whenLastVoted = LocalDateTime.now().toString();
				stmt.addBatch("update fopzlvote_playerstats set totalVotes = totalVotes + 1 where uuid = '" + uuid + "';");
				stmt.addBatch("update fopzlvote_playerstats set whenLastVoted = '" + LocalDateTime.now().toString() + "' where uuid = '" + uuid + "';");
				stmt.addBatch("update fopzlvote_playerHist set numVotes = numVotes + 1 where uuid = '" + uuid + "' and year = " + year + " and month = " + month + ";");
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
