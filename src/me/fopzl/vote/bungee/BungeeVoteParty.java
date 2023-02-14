package me.fopzl.vote.bungee;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.bungee.util.Util;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.config.Configuration;

public class BungeeVoteParty {
	private static int pointsToStart, points, notifyInterval;
	private static String notifyMsg;
	private static HashMap<Integer, String> specificNotifies = new HashMap<Integer, String>();
	private static HashMap<String, Integer> servers = new HashMap<String, Integer>();
	
	public static void load(Configuration sec) {
		pointsToStart = sec.getInt("points-to-start", 500);
		notifyMsg = sec.getString("notifications.interval.msg");
		notifyInterval = sec.getInt("notifications.interval.num");
		
		Configuration specificSec = sec.getSection("notifications.specific"); 
		for(String key : specificSec.getKeys()) {
			int pointNum = Integer.parseInt(key);
			String msg = specificSec.getString(key);
			specificNotifies.put(pointNum, msg);
		}
		
		Configuration serverSec = sec.getSection("servers");
		for (String key : serverSec.getKeys()) {
			servers.put(key, serverSec.getInt(key));
		}
	}
	
	public static void showStatus(CommandSender s) {
		Util.msg(s, "&e" + points + " / " + pointsToStart + " &7votes for a vote party to commence!");
	}
	
	public static int getPointsToStart() {
		return pointsToStart;
	}
	
	public static void addPoints(int pts) {
		points += pts;
		tick();
	}
	
	public static void setPoints(int pts) {
		points = pts;
	}
	
	public static int getPoints() {
		return points;
	}
	
	private static void tick() {
		if(points % notifyInterval == 0 && points != pointsToStart) {
			Util.broadcast(notifyMsg.replaceAll("%votesremaining%", "" + (pointsToStart - points)));
		}
		
		for(int i : specificNotifies.keySet()) {
			if(points == i) {
				Util.broadcast(specificNotifies.get(i).replaceAll("%votesremaining%", "" + (pointsToStart - points)));
			}
		}
		
		if (points >= pointsToStart) {
			triggerParties();
		}
	}
	
	public static void triggerParties() {
		points = 0;
		for (Entry<String, Integer> e : servers.entrySet()) {
			if (e.getValue() == 0) {
				BungeeCore.sendPluginMessage(new String[] {e.getKey()}, new String[] {"fopzlvote-startparty"}, true);
			}
			else {
				BungeeVote.inst().getProxy().getScheduler().schedule(BungeeVote.inst(), () -> {
					BungeeCore.sendPluginMessage(new String[] {e.getKey()}, new String[] {"fopzlvote-startparty"}, true);
				}, e.getValue(), TimeUnit.SECONDS);
			}
		}
	}
}
