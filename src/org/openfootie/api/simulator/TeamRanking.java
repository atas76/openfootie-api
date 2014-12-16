package org.openfootie.api.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openfootie.api.domain.Rankable;

public class TeamRanking {
	
	private Map<Integer, Rankable> ranking;
	private Map<Rankable, Integer> reverseRanking;
	
	public TeamRanking(List<? extends Rankable> teams) {
		
		int currentPosition = 1;
		
		this.ranking = new HashMap<Integer, Rankable>();
		this.reverseRanking = new HashMap<Rankable, Integer>();
		
		for (Rankable team:teams) {
			ranking.put(currentPosition, team);
			reverseRanking.put(team, currentPosition);
			currentPosition++;
		}
	}
	
	public List<List<Rankable>> getQuantiles(int quantilesNumber) {
		// Split ranked teams to quantiles
		List<List<Rankable>> teamQuantiles = new ArrayList<List<Rankable>>();
				
		double quantileSize = (double) ranking.size() / quantilesNumber;
		double currentQuantileStart = 0d;
		double currentQuantileEnd = quantileSize;
		
		// System.out.println("Quantile size: " + quantileSize);
		
		// System.out.println("Ranking size: " + ranking.size());
		
		boolean lastQuantile = false;
		while (!lastQuantile) {
			
			// System.out.println("Quantile end: " + currentQuantileEnd);
					
			if ( (int) currentQuantileEnd == this.ranking.size()) {
				// System.out.println("Last quantile");
				lastQuantile = true;
			}
					
			List<Rankable> currentQuantile = new ArrayList<Rankable>();
			
			// Implicity increment integer part by one if the quantile is not integer
			for (int i = (int) Math.ceil(currentQuantileStart); i < currentQuantileEnd; i++) {
				currentQuantile.add(this.getTeamByPosition(i + 1));
			}
					
			teamQuantiles.add(currentQuantile);
			currentQuantileStart = currentQuantileEnd;
			currentQuantileEnd += quantileSize;
					
			if (currentQuantileEnd > this.ranking.size()) {
				currentQuantileEnd = this.ranking.size();
			}
		}
		
		return teamQuantiles;
	}
	
	/*
	public void addTeamInPosition(Integer position, Rankable team) {
		
	}
	*/
	
	public Rankable getTeamByPosition(Integer position) {
		return ranking.get(position);
	}
	
	public Integer getPositionByTeam(Rankable team) {
		return reverseRanking.get(team);
	}
}
