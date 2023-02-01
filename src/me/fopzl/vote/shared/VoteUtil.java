package me.fopzl.vote.shared;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import com.alessiodp.lastloginapi.api.LastLogin;
import com.alessiodp.lastloginapi.api.interfaces.LastLoginPlayer;

import me.fopzl.vote.bukkit.VoteSiteInfo;

public class VoteUtil {
	private static HashMap<String, VoteSiteInfo> voteSites = new HashMap<String, VoteSiteInfo>();

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
	
	public static void resetVoteSites() {
		voteSites.clear();
	}
	
	public static void addVoteSite(String site, VoteSiteInfo info) {
		voteSites.put(site, info);
	}
	
	public static HashMap<String, VoteSiteInfo> getVoteSites() {
		return voteSites;
	}
	
	// Checks if a vote is viable via the provided username, null if the vote is invalid
	public static UUID checkVote(String user, String site) {
		if (voteSites.containsKey(site) || site.equalsIgnoreCase("freevote")) {
			if (!user.matches("[a-zA-Z0-9]*")) return null;
			
			Set<? extends LastLoginPlayer> potentialVoters = LastLogin.getApi().getPlayerByName(user);
			if (potentialVoters.size() == 0) {
				return null;
			}
			
			// If there's only one username match, make that the uuid
			else if (potentialVoters.size() == 1) {
				return potentialVoters.stream().findAny().get().getPlayerUUID();
			}
			
			// If there's more than one username match, use the uuid of the most recent login
			else {
				return potentialVoters.stream().max(comp).get().getPlayerUUID();
			}
		}
		return null;
	}
}
