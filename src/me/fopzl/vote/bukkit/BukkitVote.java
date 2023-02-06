package me.fopzl.vote.bukkit;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import me.fopzl.vote.bukkit.commands.*;
import me.fopzl.vote.bukkit.io.BukkitVoteIO;
import me.fopzl.vote.bukkit.io.VoteStats;
import me.fopzl.vote.shared.VoteUtil;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.bungee.PluginMessageEvent;
import me.neoblade298.neocore.bukkit.commands.SubcommandManager;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import net.md_5.bungee.api.ChatColor;

public class BukkitVote extends JavaPlugin implements Listener {

	private static VoteRewards rewards;
	private static VoteParty voteParty;


	private static BukkitVote instance;
	public static boolean debug = false;

	public void onEnable() {
		super.onEnable();

		instance = this;

		rewards = new VoteRewards();
		voteParty = new VoteParty();

		getServer().getPluginManager().registerEvents(this, this);

		NeoCore.registerIOComponent(this, new BukkitVoteIO(), "FoPzlVoteIO");

		loadAllConfigs();
		
		initCommands();

		Bukkit.getServer().getLogger().info("FoPzlVote Enabled");
	}
	
	private void initCommands() {
		SubcommandManager mngr = new SubcommandManager("vote", null, ChatColor.RED, this);
		mngr.register(new CmdVote("", "Use to vote!", null, SubcommandRunner.PLAYER_ONLY));
		mngr.registerCommandList("help");
		mngr.register(new CmdVoteCooldown("cooldown", "Shows vote site cooldowns", null, SubcommandRunner.BOTH));
		mngr.register(new CmdVoteLeaderboard("leaderboard", "Shows top voters of the month", null, SubcommandRunner.BOTH));
		mngr.register(new CmdVoteStats("stats", "Shows vote stats", null, SubcommandRunner.BOTH));
		
		mngr = new SubcommandManager("adminvote", "fopzlvote.admin", ChatColor.DARK_RED, this);
		mngr.registerCommandList("");
		mngr.register(new CmdAdminVoteDebug("debug", "Toggle debug mode", null, SubcommandRunner.BOTH));
		mngr.register(new CmdAdminVoteReload("reload", "Reloads config", null, SubcommandRunner.BOTH));
		mngr.register(new CmdAdminVoteReward("reward", "Gives a reward to a player", null, SubcommandRunner.BOTH));
		mngr.register(new CmdAdminVoteSetstreak("setstreak", "Sets a player's streak", null, SubcommandRunner.BOTH));
		mngr.register(new CmdAdminVoteVote("vote", "Sends a test vote", null, SubcommandRunner.BOTH));
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public static void onVote(final VotifierEvent e) {
		Vote vote = e.getVote();
		String site = vote.getServiceName();
		String user = vote.getUsername();
		UUID uuid = VoteUtil.checkVote(user, site);
		if (uuid == null) {
			Bukkit.getLogger().warning("[FoPzlVote] Vote failed, invalid username " + user);
			return;
		}

		new BukkitRunnable() {
			public void run() {
				VoteStats stats = BukkitVoteIO.loadOrGetStats(uuid);
				if (stats == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Vote failed, could not load stats for " + user);
					return;
				}
				new BukkitRunnable() {
					public void run() {
						stats.handleVote(site);
					}
				}.runTask(instance);
			}
		}.runTaskAsynchronously(instance);
	}
	
	@EventHandler
	public static void onPluginMsg(PluginMessageEvent e) {
		if (e.getChannel().equals("fopzlvote-startparty")) {
			VoteParty.startCountdown();
		}
	}

	public void onDisable() {
		Bukkit.getServer().getLogger().info("FoPzlVote Disabled");
		super.onDisable();
	}

	public static BukkitVote getInstance() {
		return instance;
	}

	public static void loadAllConfigs() {
		File mainCfg = new File(instance.getDataFolder(), "config.yml");
		if (!mainCfg.exists()) {
			instance.saveResource("config.yml", false);
		}
		instance.reload(YamlConfiguration.loadConfiguration(mainCfg));
		voteParty.reload(YamlConfiguration.loadConfiguration(mainCfg));

		File rewardsCfg = new File(instance.getDataFolder(), "rewards.yml");
		if (!rewardsCfg.exists()) {
			instance.saveResource("rewards.yml", false);
		}
		rewards.reload(YamlConfiguration.loadConfiguration(rewardsCfg));
	}

	public void reload(YamlConfiguration cfg) {
		VoteUtil.resetVoteSites();

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

			VoteUtil.addVoteSite(vsi.nickname, vsi);
		}

		VoteStats.setStreakLimit(cfg.getInt("streak-vote-limit"));
		VoteStats.setStreakResetTime(cfg.getInt("streak-reset-leniency"));
	}

	public static VoteParty getVoteParty() {
		return voteParty;
	}
}
