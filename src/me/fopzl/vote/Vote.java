package me.fopzl.vote;

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
import org.bukkit.plugin.java.JavaPlugin;

import com.vexsoftware.votifier.google.gson.JsonObject;
import com.vexsoftware.votifier.model.VotifierEvent;

import me.fopzl.vote.commands.MLVoteCommand;
import me.fopzl.vote.commands.VotePartyCommand;

public class Vote extends JavaPlugin {
	private VoteListener voteListener;
	
	private VoteRewards rewards;
	private VoteParty voteParty;
	
	private Map<String, VoteSiteInfo> voteSites; // key is servicename, not nickname
	
	private VoteInfo info;
	private VoteIO io;
	
	private static Vote instance;
	
	public void onEnable() {
		super.onEnable();
		
		rewards = new VoteRewards();
		voteParty = new VoteParty(this);
		
		info = new VoteInfo(this);
		io = new VoteIO(this);

		voteListener = new VoteListener(this);
		getServer().getPluginManager().registerEvents(voteListener, this);

		MLVoteCommand mlvoteCmd = new MLVoteCommand(this);
		this.getCommand("mlvote").setExecutor(mlvoteCmd);
		this.getCommand("mlvote").setTabCompleter(mlvoteCmd);
		VotePartyCommand vpCmd = new VotePartyCommand(voteParty);
		this.getCommand("voteparty").setExecutor(vpCmd);
		this.getCommand("voteparty").setTabCompleter(vpCmd);
		
		loadAllConfigs();
		
		instance = this;
		
		Bukkit.getServer().getLogger().info("FoPzlVote Enabled");
	}
	
	public void onDisable() {
		io.saveQueue();
		
		Bukkit.getServer().getLogger().info("FoPzlVote Disabled");
		super.onDisable();
	}
	
	public static Vote getInstance() {
		return instance;
	}
	
	public void loadAllConfigs() {
		File mainCfg = new File(getDataFolder(), "config.yml");
		if(!mainCfg.exists()) {
			saveResource("config.yml", false);
		}
		this.loadConfig(YamlConfiguration.loadConfiguration(mainCfg));

		File rewardsCfg = new File(getDataFolder(), "rewards.yml");
		if(!rewardsCfg.exists()) {
			saveResource("rewards.yml", false);
		}
		rewards.loadConfig(YamlConfiguration.loadConfiguration(rewardsCfg));	
		
		File votepartyCfg = new File(getDataFolder(), "voteparty.yml");
		if(!votepartyCfg.exists()) {
			saveResource("voteparty.yml", false);
		}
		voteParty.loadConfig(YamlConfiguration.loadConfiguration(votepartyCfg));
	}
	
	public void loadConfig(YamlConfiguration cfg) {
		voteSites = new HashMap<String, VoteSiteInfo>();
		
		ConfigurationSection siteSec = cfg.getConfigurationSection("websites");
		for(String siteNick : siteSec.getKeys(false)) {
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
	
	public VoteInfo getVoteInfo() {
		return info;
	}
	
	public VoteIO getVoteIO() {
		return io;
	}
	
	public VoteParty getVoteParty() {
		return voteParty;
	}
	
	public void cmdVote(String username, String serviceName) {
        JsonObject o = new JsonObject();
        o.addProperty("serviceName", serviceName);
        o.addProperty("username", username);
        o.addProperty("address", "xxx");
        o.addProperty("timestamp", "xxx");

		voteListener.onVote(new VotifierEvent(new com.vexsoftware.votifier.model.Vote(o)));
	}
	
	public void incVoteParty() {
		voteParty.addPoints(1);
	}
	
	public void showStats(CommandSender showTo, Player player) {
		VoteStats stats = info.getStats(player);
		
		String msg = "&4[&c&lMLMC&4] &eVote Stats for &6" + player.getName() + "&e:"
				+ "\n &eAll time votes: &7" + stats.getTotalVotes()
				+ "\n &eVotes this month: &7" + stats.getVotesThisMonth()
				+ "\n &eCurrent Streak: &7" + stats.getStreak();
		
		Util.sendMessageFormatted(showTo, msg);
	}
	
	public void showCooldowns(CommandSender showTo, OfflinePlayer player) {
		String msg = "&4[&c&lMLMC&4] &eVote Site Cooldowns for &6" + player.getName() + "&e:";
		for(Entry<String, VoteSiteInfo> site : voteSites.entrySet()) {
			String cd = getCooldown(player, site.getKey());
			msg += "\n &e" + site.getValue().nickname + ": " + cd;
		}
		
		Util.sendMessageFormatted(showTo, msg);
	}
	
	public void showLeaderboard(CommandSender sender) {
		List<Object[]> topVoters = io.getTopVoters(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
		int num = 1;
		String msg = "&4[&c&lMLMC&4] &eTop Monthly Voters:";
		for(Object[] entry : topVoters) {
			String username = Bukkit.getServer().getOfflinePlayer((UUID)entry[0]).getName();
			msg += "\n&6&l" + num++ + ". &e" + username + " &7- &f" + (int)entry[1];
		}
		
		Util.sendMessageFormatted(sender, msg);
	}
	
	public boolean isValidSite(String voteServiceName) {
		return voteSites.containsKey(voteServiceName);
	}
	
	public void rewardVote(Player p, String voteServiceName) {
		VoteStats stats = info.getStats(p);
		rewards.rewardVote(p, stats.voteStreak);
	}
	
	public void countVote(OfflinePlayer p, String voteServiceName) {
		VoteStats stats = info.getStats(p);
		String nickname;
		if(voteSites.containsKey(voteServiceName)) {
			nickname = voteSites.get(voteServiceName).nickname;
		} else {
			nickname = voteServiceName;
		}
		stats.addVote(nickname);
	}
	
	public void setCooldown(OfflinePlayer player, String voteServiceName) {
		io.setCooldown(player, voteSites.get(voteServiceName).nickname);
	}
	
	public String getCooldown(OfflinePlayer player, String voteServiceName) {
		VoteSiteInfo vsi = voteSites.get(voteServiceName);
		LocalDateTime lastVoted = io.getCooldown(player, vsi.nickname);
		
		Duration dur = vsi.cooldown.getCooldownRemaining(lastVoted);
		
		if(dur.isNegative()) return "&aReady!";
		else return String.format("&c%02d:%02d:%02d", dur.toHours(), dur.toMinutesPart(), dur.toSecondsPart());
	}
	
	public void setTotalVotes(Player p, int numVotes) {
		info.getStats(p).totalVotes = numVotes;
		info.getStats(p).needToSave = true;
	}

	public boolean giveReward(String username, String rewardName) {
		return rewards.giveReward(Bukkit.getServer().getPlayer(username), rewardName);
	}
}

