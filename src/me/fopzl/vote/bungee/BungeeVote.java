package me.fopzl.vote.bungee;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.alessiodp.lastloginapi.api.LastLogin;
import com.alessiodp.lastloginapi.api.interfaces.LastLoginPlayer;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;

import me.fopzl.vote.SpigotVote;
import me.fopzl.vote.VoteCooldown;
import me.fopzl.vote.VoteSiteInfo;
import me.fopzl.vote.io.VoteIO;
import me.neoblade298.bungeecore.BungeeCore;
import me.neoblade298.bungeecore.util.BUtil;
import me.neoblade298.neocore.util.CachedObject;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeVote extends Plugin implements Listener
{
	private static HashMap<String, VoteSiteInfo> voteSites = new HashMap<String, VoteSiteInfo>();
	private static HashMap<UUID, CachedObject<LocalDateTime>> lastVoted = new HashMap<UUID, CachedObject<LocalDateTime>>();
	private static BungeeVote inst;
	
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
        getProxy().getPluginManager().registerListener(this, this);
        new VoteIO(true);
        inst = this;
        
        // Scheduler for clearing cache
        getProxy().getScheduler().schedule(this, () -> {
        	for (Entry<UUID, CachedObject<LocalDateTime>> ent : lastVoted.entrySet()) {
        		if (ent.getValue().cachedLongerThan(Duration.of(15, ChronoUnit.MINUTES))) {
        			lastVoted.remove(ent.getKey());
        		}
        	}
        }, 0, 15, TimeUnit.MINUTES);
        
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
	public static void onVote(VotifierEvent e) {
		Vote vote = e.getVote();
		String site = vote.getServiceName();
		String user = vote.getUsername();
		if (voteSites.containsKey(site) || site.equalsIgnoreCase("freevote")) {
			if (!user.matches("[a-zA-Z0-9]*")) {
				BungeeVote.inst().getProxy().getLogger().warning("[FopzlVote] Vote failed due to invalid username: " + user);
				return;
			}
			
			BUtil.broadcast("&e" + user + " &7just voted on &c" + site + "&7!");
			
			// If the player is online, do nothing. Otherwise, manually update VoteStatsGlobal sql
			UUID uuid;
			String activeServer = null;
			if (inst.getProxy().getPlayer(user) == null) {
				Set<? extends LastLoginPlayer> potentialVoters = LastLogin.getApi().getPlayerByName(user);
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
				
				// Manually save to sql
			}
			else {
				ProxiedPlayer p = inst.getProxy().getPlayer(user);
				activeServer = p.getServer().getInfo().getName();
				uuid = p.getUniqueId();
			}

			// Check if streak needs to be reset, cache lastVoted
			try {
				Statement insert = BungeeCore.getPluginStatement("FoPzlVote");
				Statement delete = BungeeCore.getPluginStatement("FoPzlVote");
				
				// Check if last vote is cached
				LocalDateTime lastVoted;
				if (BungeeVote.lastVoted.containsKey(uuid)) {
					lastVoted = BungeeVote.lastVoted.get(uuid).get();
				}
				else {
					ResultSet rs = insert.executeQuery("SELECT whenLastVoted FROM fopzlvote_playerStats");
					lastVoted = rs.next() ? rs.getObject(1, LocalDateTime.class) : LocalDateTime.now();
				}
				
				// If streak is lost, update sql on every server EXCEPT the active one (which will handle it on its own)
				if (Duration.between(lastVoted, LocalDateTime.now()).compareTo(Duration.ofDays(2)) > 0) {
					String prefix = "INSERT INTO fopzlvote_prevStreaks (SELECT uuid, server, voteStreak, numQueued FROM fopzlvote_voteQueue";
					String suffix = ");";
					if (activeServer != null) {
						insert.addBatch(prefix + " WHERE server != " + activeServer + suffix);
					}
					else {
						insert.addBatch(prefix + suffix);
					}
					// Delete everything, the active server will re-save anyway
					delete.addBatch("DELETE FROM fopzlvote_voteQueue WHERE uuid = " + uuid);
				}
				VoteIO.setCooldown(insert, uuid, voteSites.get(site).nickname);
				insert.executeBatch();
				delete.executeBatch();
			}
			catch (SQLException ex) {
				ex.printStackTrace();
			}
			
			BungeeVoteParty.addPoints(1);
		}
	}
	
	public static BungeeVote inst() {
		return inst;
	}
}
