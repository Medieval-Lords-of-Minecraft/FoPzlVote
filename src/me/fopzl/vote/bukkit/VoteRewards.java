package me.fopzl.vote.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.fopzl.vote.bukkit.io.VoteStats;

public class VoteRewards {
	private Map<String, Reward> allRewards;

	private Reward dailyReward;
	private Map<Integer, Set<Reward>> streakRewards;
	
	public void reload(YamlConfiguration cfg) {
		allRewards = new HashMap<String, Reward>();
		
		ConfigurationSection sec = cfg.getConfigurationSection("rewards");
		for(String rName : sec.getKeys(false)) {
			allRewards.put(rName, new RawReward(sec.getString(rName)));
		}
		
		ConfigurationSection groupSec = cfg.getConfigurationSection("groups");
		for(String gName : groupSec.getKeys(false)) {
			allRewards.put(gName, new RewardGroup());
		}
		
		ConfigurationSection poolSec = cfg.getConfigurationSection("pools");
		for(String pName : poolSec.getKeys(false)) {
			allRewards.put(pName, new RewardPool());
		}
		
		ConfigurationSection restrictSec = cfg.getConfigurationSection("permissioned-groups");
		for(String rName : restrictSec.getKeys(false)) {
			allRewards.put(rName, new RestrictedReward());
		}
		
		for(String groupName : groupSec.getKeys(false)) {
			RewardGroup g = (RewardGroup)allRewards.get(groupName);
			
			for(String groupItem : groupSec.getStringList(groupName)) {
				g.addReward(allRewards.get(groupItem));
			}
		}
		
		for(String poolName : poolSec.getKeys(false)) {
			RewardPool p = (RewardPool)allRewards.get(poolName);
			
			ConfigurationSection subSec = poolSec.getConfigurationSection(poolName);
			for(String poolItem : subSec.getKeys(false)) {
				p.addReward(allRewards.get(poolItem), subSec.getInt(poolItem));
			}
		}
		
		for(String permGroupName : restrictSec.getKeys(false)) {
			RestrictedReward r = (RestrictedReward)allRewards.get(permGroupName);
			
			for(Object o : restrictSec.getList(permGroupName)) {
				@SuppressWarnings("unchecked")
				Entry<String, String> permGroupItem = ((Map<String, String>)o).entrySet().iterator().next();
				String permName = permGroupItem.getKey();
				String rewardName = permGroupItem.getValue();
				r.addReward(allRewards.get(rewardName), permName);
			}
		}
		
		streakRewards = new HashMap<Integer, Set<Reward>>();
		ConfigurationSection streakSec = cfg.getConfigurationSection("streaks");
		for(String s : streakSec.getKeys(false)) {
			int streakNum = Integer.parseInt(s);
			Set<Reward> itemSet = streakRewards.getOrDefault(streakNum, new HashSet<Reward>());
			streakRewards.putIfAbsent(streakNum, itemSet);
			
			for(String streakItem : streakSec.getStringList(s)) {
				itemSet.add(allRewards.get(streakItem));
			}
		}
		
		dailyReward = allRewards.get(cfg.getString("daily"));
	}
	
	// streak is in votes, not days
	public void rewardVote(Player p, VoteStats stats) {
		dailyReward.giveReward(p);
		int streak = stats.getStreak();
		
		if(streakRewards.containsKey(streak)) {
			for(Reward r : streakRewards.get(streak)) {
				if (r == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Failed to give a reward for streak " + streak);
					continue;
				}
				r.giveReward(p);
			}
		}
		Bukkit.getLogger().info("[FoPzlVote] Gave player " + p.getName() + " reward for streak " + streak);
	}

	public boolean giveReward(Player p, String rewardName) {
		if(allRewards.containsKey(rewardName)) {
			allRewards.get(rewardName).giveReward(p);
			return true;
		} else return false;
	}
}

interface Reward {
	void giveReward(Player p);
}

// single reward given
class RawReward implements Reward {
	private String command;
	
	public RawReward(String cmd) {
		command = cmd;
	}

	public void giveReward(Player p) {
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", p.getName()));
	}
}

// all rewards in group given
class RewardGroup implements Reward {
	private Set<Reward> rewards = new HashSet<Reward>();
	
	public void addReward(Reward r) {
		rewards.add(r);
	}
	
	public void giveReward(Player p) {
		for(Reward r : rewards) {
			r.giveReward(p);
		}
	}
}

// one random reward is given
class RewardPool implements Reward {
	private static Random rng = new Random();
	
	private List<Object[]> lootTable = new ArrayList<Object[]>();
	private int sumWeights = 0;
	
	public void addReward(Reward reward, int weight) {
		lootTable.add(new Object[] { reward, weight }); // thank you java
		sumWeights += weight;
	}
	
	public void giveReward(Player p) {
		int choice = rng.nextInt(sumWeights);
		
		int index = -1;		
		while (choice >= 0) {
			choice -= (int)lootTable.get(++index)[1];
		}
		
		((Reward)lootTable.get(index)[0]).giveReward(p);
	}
}

//first authorized reward is given
class RestrictedReward implements Reward {
	private List<Object[]> rewards = new ArrayList<Object[]>(); // ordered by decreasing authority
	
	public void addReward(Reward reward, String permission) {
		rewards.add(new Object[]{ reward, permission }); // thank you java
	}
	
	public void giveReward(Player p) {
		for(Object[] tuple : rewards) {
			String permission = (String)tuple[1];
			if(permission.equalsIgnoreCase("default") || p.hasPermission(permission)) {
				((Reward)tuple[0]).giveReward(p);
				return;
			}
		}
	}
}