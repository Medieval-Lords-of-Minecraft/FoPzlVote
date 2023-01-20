package me.fopzl.vote;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class VoteCooldown {
	private boolean fixedReset; // if true, cooldown resets at a fixed time each day; else, cooldown resets after some amount of time since last vote
	private int resetTime; // for a fixed reset, this is the 0-indexed hour of day; otherwise, this is the number of hours needed to wait after last vote
	private String timezone; // only needed for a fixed reset
	
	public VoteCooldown(boolean fixedReset, int resetTime) {
		this.fixedReset = fixedReset;
		this.resetTime = resetTime;
		timezone = null;
	}
	
	public VoteCooldown(boolean fixedReset, int resetTime, String timezone) {
		this.fixedReset = fixedReset;
		this.resetTime = resetTime;
		this.timezone = timezone;
	}
	
	public LocalDateTime getWhenOffCooldown(LocalDateTime whenLastVoted) {
		if(fixedReset) {
			int trueResetTime = resetTime + getTimezoneOffset(timezone);
			if(trueResetTime < 0) trueResetTime += 24;
					
			if(whenLastVoted.getHour() < trueResetTime) {
				return whenLastVoted.withHour(trueResetTime).withMinute(0).withSecond(0).withNano(0);
			} else {
				return whenLastVoted.plusDays(1).withHour(trueResetTime).withMinute(0).withSecond(0).withNano(0);
			}
		} else {
			return whenLastVoted.plusHours(resetTime);
		}
	}
	
	public Duration getCooldownRemaining(LocalDateTime whenLastVoted) {
		return Duration.between(LocalDateTime.now(), getWhenOffCooldown(whenLastVoted));
	}
	
	// offset from America/New_York (MLMC host timezone)
	private static int getTimezoneOffset(String timezone) {
		return ZoneId.of("America/New_York").getRules().getOffset(Instant.now()).getTotalSeconds() / 3600 -
				ZoneId.of(timezone).getRules().getOffset(Instant.now()).getTotalSeconds() / 3600;
	}
}
