package me.fopzl.vote.bungee;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.alessiodp.lastloginapi.api.LastLogin;
import com.alessiodp.lastloginapi.api.interfaces.LastLoginPlayer;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;

import me.fopzl.vote.Vote;
import me.fopzl.vote.io.VoteIO;
import me.fopzl.vote.io.VoteInfo;
import me.fopzl.vote.io.VoteStatsGlobal;
import me.fopzl.vote.io.VoteStatsLocal;
import me.neoblade298.bungeecore.util.BUtil;
import me.neoblade298.neocore.bungee.BungeeAPI;
import me.neoblade298.neoleaderboard.points.PlayerEntry;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeVote extends Plugin implements Listener
{
	private static HashMap<UUID, VoteStatsGlobal> globalStats = new HashMap<UUID, VoteStatsGlobal>();
	private static HashMap<String, VoteSiteInfo> voteSites = new HashMap<String, VoteSiteInfo>();
	private static BungeeVote inst;

	private static int pointsForVoteParty;
	
	private static Comparator<LastLoginPlayer> comp = new Comparator<LastLoginPlayer>() {
		@Override
		public int compare(LastLoginPlayer p1, LastLoginPlayer p2) {
			if (p1.getLastLogout() > p2.getLastLogout()) {
				return 1;
			}
			else if (p1.getLastLogout() < p2.getLastLogout()) {
				return -1;
			}
			else {
				return 0;
			}
		}
	};
    @Override
    public void onEnable() {
        // getProxy().getPluginManager().registerCommand(this, new CmdHub());
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("neocore:bungee");
        new VoteIO();
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
			
			voteSites.put(vsi.serviceName, vsi);
		}
		
		ConfigurationSection sec = cfg.getConfigurationSection("voteparty");
		new BungeeVoteParty(sec);
	}

	@EventHandler
	public static void onVote(final VotifierEvent e) {
		com.vexsoftware.votifier.model.Vote vote = e.getVote();
		String site = vote.getServiceName();
		String user = vote.getUsername();
		if (Vote.isValidSite(site) || site.equalsIgnoreCase("freevote")) {
			if (!user.matches("[a-zA-Z0-9]*")) {
				BungeeVote.inst().getProxy().getLogger().warning("[FopzlVote] Vote failed due to invalid username: " + user);
				return;
			}
			
			BUtil.broadcast("&e" + user + " &7just voted on &c" + site + "&7!");
			
			// Get a list of UUIDs that it could be
			Set<? extends LastLoginPlayer> potentialVoters = LastLogin.getApi().getPlayerByName(user);
			UUID uuid;
			if (potentialVoters.size() == 0) {
				BungeeVote.inst().getProxy().getLogger().warning("[FopzlVote] Vote failed due to username never having logged on before: " + user);
				return;
			}
			
			// If there's only one username match, make that the uuid
			else if (potentialVoters.size() == 1) {
				uuid = potentialVoters.stream().findAny().get().getPlayerUUID();
			}
			
			// If there's more than one username match, use the uuid of the most recent login
			else {
				uuid = potentialVoters.stream().max(comp).get().getPlayerUUID();
			}
			countVote(uuid, site);
			
			/*
			if(p.isOnline()) {
				main.rewardVote(p.getPlayer());
			} else {
				UUID uuid = p.getUniqueId();
				VoteIO.addOfflinePlayer(uuid);
				Map<String, Integer> pq = null;
				Map<UUID, Map<String, Integer>> qr = VoteInfo.queuedRewards;
				
				if(!qr.containsKey(uuid)) {
					pq = new HashMap<String, Integer>();
					qr.put(uuid, pq);
				} else {
					pq = qr.get(uuid);
				}
				pq.put(site, pq.getOrDefault(site, 0) + 1);
			}
			
			main.incVoteParty();
			main.setCooldown(p, site);
			*/
		}
	}
	
	public static void countVote(UUID uuid, String voteServiceName) {
		VoteStatsGlobal stats = loadGlobalStats(uuid);
		String nickname;
		if(voteSites.containsKey(voteServiceName)) {
			nickname = voteSites.get(voteServiceName).nickname;
		} else {
			nickname = voteServiceName;
		}
		stats.addVote(nickname);
	}
	
	private static VoteStatsGlobal loadGlobalStats(UUID uuid) {
		if (globalStats.containsKey(uuid)) {
			return globalStats.get(uuid);
		}
		else {
			VoteIO.tryLoadGlobalStats(uuid);
		}
	}
	
	public static BungeeVote inst() {
		return inst;
	}
}
