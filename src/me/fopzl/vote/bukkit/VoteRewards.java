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

public class VoteRewards {
	private static Map<String, Reward> allRewards;

	private static Reward dailyReward;
	private static Map<Integer, Set<Reward>> streakRewards;
	
	public static void reload(YamlConfiguration cfg) {
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
				Reward r = allRewards.get(groupItem);
				if (r == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Failed to load reward " + groupItem + " for group " + groupName);
					continue;
				}
				g.addReward(r);
			}
		}
		
		for(String poolName : poolSec.getKeys(false)) {
			RewardPool p = (RewardPool)allRewards.get(poolName);
			
			ConfigurationSection subSec = poolSec.getConfigurationSection(poolName);
			for(String poolItem : subSec.getKeys(false)) {
				Reward r = allRewards.get(poolItem);
				if (r == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Failed to load reward " + poolItem + " for pool " + poolName);
					continue;
				}
				p.addReward(r, subSec.getInt(poolItem));
			}
		}
		
		for(String permGroupName : restrictSec.getKeys(false)) {
			RestrictedReward rr = (RestrictedReward)allRewards.get(permGroupName);
			
			for(Object o : restrictSec.getList(permGroupName)) {
				@SuppressWarnings("unchecked")
				Entry<String, String> permGroupItem = ((Map<String, String>)o).entrySet().iterator().next();
				String permName = permGroupItem.getKey();
				String rewardName = permGroupItem.getValue();
				Reward r = allRewards.get(rewardName);
				if (r == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Failed to load reward " + rewardName + " for permission group " + permName);
					continue;
				}
				rr.addReward(r, permName);
			}
		}
		
		streakRewards = new HashMap<Integer, Set<Reward>>();
		ConfigurationSection streakSec = cfg.getConfigurationSection("streaks");
		for(String s : streakSec.getKeys(false)) {
			int streakNum = Integer.parseInt(s);
			Set<Reward> itemSet = streakRewards.getOrDefault(streakNum, new HashSet<Reward>());
			streakRewards.putIfAbsent(streakNum, itemSet);
			
			for(String streakItem : streakSec.getStringList(s)) {
				Reward r = allRewards.get(streakItem);
				if (r == null) {
					Bukkit.getLogger().warning("[FoPzlVote] Failed to load reward " + streakItem + " for streak " + streakNum);
					continue;
				}
				itemSet.add(r);
			}
		}
		
		dailyReward = allRewards.get(cfg.getString("daily"));
	}
	
	public static void rewardVotes(Player p, int streak, int queued) {
		for (int i = streak + 1; i <= streak + queued; i++) {
			dailyReward.giveReward(p);
			
			if(streakRewards.containsKey(i)) {
				for(Reward r : streakRewards.get(i)) {
					if (r == null) {
						Bukkit.getLogger().warning("[FoPzlVote] Failed to give a reward for streak " + i);
						continue;
					}
					r.giveReward(p);
				}
			}
			Bukkit.getLogger().info("[FoPzlVote] Gave player " + p.getName() + " reward for streak " + i);
		}
	}

	public static boolean giveReward(Player p, String rewardName) {
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