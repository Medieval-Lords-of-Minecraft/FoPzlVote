package me.fopzl.vote.bukkit;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import me.fopzl.vote.bukkit.commands.MLVoteCommand;
import me.fopzl.vote.bukkit.io.BukkitVoteIO;
import me.fopzl.vote.bukkit.io.VoteStats;
import me.fopzl.vote.shared.VoteUtil;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;

public class BukkitVote extends JavaPlugin implements Listener {

	private static VoteRewards rewards;
	private static VoteParty voteParty;

	private static Map<String, VoteSiteInfo> voteSites; // key is servicename, not nickname

	private static BukkitVote instance;
	public static boolean debug = false;

	public void onEnable() {
		super.onEnable();

		rewards = new VoteRewards();
		voteParty = new VoteParty();

		getServer().getPluginManager().registerEvents(this, this);

		MLVoteCommand mlvoteCmd = new MLVoteCommand(this);
		this.getCommand("mlvote").setExecutor(mlvoteCmd);
		this.getCommand("mlvote").setTabCompleter(mlvoteCmd);

		NeoCore.registerIOComponent(this, new BukkitVoteIO(), "FoPzlVoteIO");

		loadAllConfigs();

		instance = this;

		Bukkit.getServer().getLogger().info("FoPzlVote Enabled");
		
	}

	@EventHandler
	public static void onVote(final VotifierEvent e) {
		Vote vote = e.getVote();
		String site = vote.getServiceName();
		String user = vote.getUsername();
		UUID uuid = VoteUtil.checkVote(user, site);
		if (uuid == null) {
			Bukkit.getLogger().warning("[FoPzlVote] Vote failed, invalid username " + user);
		}

		Player p = Bukkit.getPlayer(user);
		new BukkitRunnable() {
			public void run() {
				VoteStats stats = BukkitVoteIO.loadOrGetStats(p.getUniqueId());
				if (stats == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Vote failed, could not load stats for " + user);
					return;
				}
				stats.handleVote(site);
			}
		}.runTaskAsynchronously(BukkitVote.instance);
	}

	public void onDisable() {
		// TODO BukkitVoteIO.saveQueue();

		Bukkit.getServer().getLogger().info("FoPzlVote Disabled");
		super.onDisable();
	}

	public static BukkitVote getInstance() {
		return instance;
	}

	public void loadAllConfigs() {
		File mainCfg = new File(getDataFolder(), "config.yml");
		if (!mainCfg.exists()) {
			saveResource("config.yml", false);
		}
		this.reload(YamlConfiguration.loadConfiguration(mainCfg));

		File rewardsCfg = new File(getDataFolder(), "rewards.yml");
		if (!rewardsCfg.exists()) {
			saveResource("rewards.yml", false);
		}
		rewards.reload(YamlConfiguration.loadConfiguration(rewardsCfg));

		File votepartyCfg = new File(getDataFolder(), "voteparty.yml");
		if (!votepartyCfg.exists()) {
			saveResource("voteparty.yml", false);
		}
		voteParty.reload(YamlConfiguration.loadConfiguration(votepartyCfg));
	}

	public void reload(YamlConfiguration cfg) {
		voteSites = new HashMap<String, VoteSiteInfo>();

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

		VoteStats.setStreakLimit(cfg.getInt("streak-vote-limit"));
		VoteStats.setStreakResetTime(cfg.getInt("streak-reset-leniency"));
	}

	public static VoteParty getVoteParty() {
		return voteParty;
	}

	public static void cmdVote(String username, String serviceName) {
		/*
		 * JsonObject o = new JsonObject(); o.addProperty("serviceName", serviceName);
		 * o.addProperty("username", username); o.addProperty("address", "xxx");
		 * o.addProperty("timestamp", "xxx");
		 * 
		 * VoteListener.onVote(new VotifierEvent(new
		 * com.vexsoftware.votifier.model.Vote(o)));
		 */
	}

	public static void showStats(CommandSender showTo, Player player) {
		VoteStats stats = BukkitVoteIO.stats.get(player.getUniqueId());

		String msg = "&eVote Stats for &6" + player.getName() + "&e:" + "\n &eAll time votes: &7"
				+ stats.getTotalVotes() + "\n &eVotes this month: &7" + stats.getVotesThisMonth()
				+ "\n &eCurrent Streak: &7";// + stats.getStreak(); TODO

		Util.msg(showTo, msg);
	}

	public static void showCooldowns(CommandSender showTo, OfflinePlayer player) {
		String msg = "&eVote Site Cooldowns for &6" + player.getName() + "&e:";
		for (Entry<String, VoteSiteInfo> site : voteSites.entrySet()) {
			String cd = getCooldown(player.getUniqueId(), site.getKey());
			msg += "\n &e" + site.getValue().nickname + ": " + cd;
		}

		Util.msg(showTo, msg);
	}

	public static void showLeaderboard(CommandSender sender) {
		List<Object[]> topVoters = BukkitVoteIO.getTopVoters(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
		int num = 1;
		String msg = "&eTop Monthly Voters:";
		for (Object[] entry : topVoters) {
			String username = Bukkit.getServer().getOfflinePlayer((UUID) entry[0]).getName();
			msg += "\n&6&l" + num++ + ". &e" + username + " &7- &f" + (int) entry[1];
		}

		Util.msg(sender, msg);
	}

	public static void rewardVote(Player p) {
		// TODO
		// VoteStatsGlobal stats = VoteStats.getLocalStats(p.getUniqueId());
		// rewards.rewardVote(p, stats);
	}

	public static void rewardVoteQueued(Player p, int queuedVotes) {
		/*
		 * VoteStatsGlobal stats = VoteInfo.getGlobalStats(p); for (int i =
		 * stats.voteStreak - queuedVotes + 1; i <= stats.voteStreak; i++) {
		 * rewards.rewardVote(p, i); }
		 */
	}

	public static String getCooldown(UUID uuid, String voteServiceName) {
		VoteSiteInfo vsi = voteSites.get(voteServiceName);
		LocalDateTime lastVoted = BukkitVoteIO.stats.get(uuid).getLastVoted();

		Duration dur = vsi.cooldown.getCooldownRemaining(lastVoted);

		if (dur.isNegative())
			return "&aReady!";
		else
			return String.format("&c%02d:%02d:%02d", dur.toHours(), dur.toMinutesPart(), dur.toSecondsPart());
	}

	public static void setTotalVotes(Player p, int numVotes) {
		/*
		 * VoteInfo.getGlobalStats(p).totalVotes = numVotes;
		 * VoteInfo.getGlobalStats(p).needToSave = true;
		 */
		// TODO
	}

	public static boolean giveReward(String username, String rewardName) {
		return rewards.giveReward(Bukkit.getServer().getPlayer(username), rewardName);
	}
}
