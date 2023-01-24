package me.fopzl.vote.shared.io;

public class VoteMonth {
	int yearNum;
	int monthNum;
	
	public VoteMonth(int y, int m) {
		yearNum = y;
		monthNum = m;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		
		VoteMonth other = (VoteMonth)obj;
		return other.yearNum == this.yearNum && other.monthNum == this.monthNum;
	}
	
	@Override
	public int hashCode() {
		return 31 * monthNum + yearNum;
	}
}
