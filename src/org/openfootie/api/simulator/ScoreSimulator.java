package org.openfootie.api.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openfootie.api.domain.Match;
import org.openfootie.api.domain.Rankable;
import org.openfootie.api.engine.MatchEngine;

public class ScoreSimulator implements MatchEngine {
	
	private TeamRanking teamRanking;
	private List<Match> sampleMatches;
	
	private int QUANTILES = 5; // Default value
	
	private static final double PENALTY_SHOOT_OUT_SUCCESS_RATE = 0.8d;
	
	private static Random rnd = new Random();

	public ScoreSimulator(TeamRanking teamRanking, List<Match> sampleMatches) {
		this.teamRanking = teamRanking;
		this.sampleMatches = sampleMatches;
	}
	
	void setQuantiles(int quantiles) {
		QUANTILES = quantiles;
	}
	
	public void simulateExtraTime(Match match) {
		
		Match extraTimeMatch = new Match(match.getHomeTeamName(), match.getAwayTeamName(), Match.Status.PLAYING, match.isNeutral());
		
		extraTimeMatch.play(this);
		
		int homeTeamETScore = extraTimeMatch.getHomeTeamScore() / 3;
		int awayTeamETScore = extraTimeMatch.getAwayTeamScore() / 3;
		
		
		// ** DEBUG
		// System.out.println("Extra time score: " + homeTeamETScore + " - " + awayTeamETScore);
		
		match.saveExtraTime();
		
		// ** DEBUG
		// System.out.println("Normal time score: " + match.getHomeTeamScore() + " - " + match.getAwayTeamScore());
		
		match.setHomeTeamScore(match.getHomeTeamScore() + homeTeamETScore);
		match.setAwayTeamScore(match.getAwayTeamScore() + awayTeamETScore);
	}
	
	public void simulatePenaltyShootOut(Match match) {
		
		int score1 = 0;
		int score2 = 0;
		
		for (int i = 0; i < 5; i++) {
			if (simulatePenaltyShot()) {
				score1++;
			}
			if (isDecided(score1, score2, 4 - i, 5 - i)) {
				break;
			}
			if (simulatePenaltyShot()) {
				score2++;
			}
			if (isDecided(score1, score2, 4 - i, 4 - i)) {
				break;
			}
		}
		if (score1 == score2) {
			do {
				if (simulatePenaltyShot()) {
					score1++;
				}
				if (simulatePenaltyShot()) {
					score2++;
				}
			} while (!isDecided(score1, score2, 0, 0));
		}
		match.setHomeTeamPenaltyScore(score1);
		match.setAwayTeamPenaltyScore(score2);
		match.setDecidedOnPenalties();
	}
	
	private boolean isDecided(int score1, int score2, int rest1, int rest2) {
		return score1 - score2 > rest2 || score2 - score1 > rest1;
		
	}
	
	private boolean simulatePenaltyShot() {
		return (rnd.nextDouble() < PENALTY_SHOOT_OUT_SUCCESS_RATE);
	}
	
	public void play(Match match) {
	
		// Get match details
		String homeTeamName = match.getHomeTeamName();
		String awayTeamName = match.getAwayTeamName();
		
		/**
		 * DEBUG
		 */
		// System.out.println("Calculating: " + homeTeamName + " - " + awayTeamName);
		
		// Split ranked teams to quantiles
		
		/*
		 * DEBUG
		 */
		
		// System.out.println("Home/Away sample size: " + filteredHASample.size());
		// debugDisplaySample(filteredHASample);
		
		// System.out.println("Neutral sample size: " + filteredNSample.size());
		// debugDisplaySample(filteredNSample);
		
		// System.out.println("Pseudo-neutral sample size: " + filteredPNSample.size());
		// debugDisplaySample(filteredPNSample);
		
		// System.out.println("Reverse-neutral sample size: " + filteredRNSample.size());
		// debugDisplaySample(filteredRNSample);
		// **
		
		for (int i = QUANTILES; i > 0; i--) {
			List<List<Rankable>> teamQuantiles = teamRanking.getQuantiles(i);
			List<List<String>> quantileRangesIndex = new ArrayList<List<String>>();
			for (List<Rankable> quantileRange : teamQuantiles) {

				List<String> currentRangeIndex = new ArrayList<String>();

				for (Rankable team : quantileRange) {
					currentRangeIndex.add(team.getName());
				}

				quantileRangesIndex.add(currentRangeIndex);
			}
			int homeTeamQuantileRange = getTeamQuantile(homeTeamName,
					quantileRangesIndex);
			int awayTeamQuantileRange = getTeamQuantile(awayTeamName,
					quantileRangesIndex);
			List<Match> filteredHASample = new ArrayList<Match>();
			List<Match> filteredNSample = new ArrayList<Match>(); // Neutral
			List<Match> filteredPNSample = new ArrayList<Match>(); // Pseudo-neutral
			List<Match> filteredRNSample = new ArrayList<Match>(); // Reverse-neutral
			// Filter match sample
			for (Match sampleMatch : sampleMatches) {

				// System.out.println(sampleMatch);

				int sampleHomeTeamQlRange = getTeamQuantile(
						sampleMatch.getHomeTeamName(), quantileRangesIndex);
				int sampleAwayTeamQlRange = getTeamQuantile(
						sampleMatch.getAwayTeamName(), quantileRangesIndex);

				if (sampleHomeTeamQlRange == homeTeamQuantileRange
						&& sampleAwayTeamQlRange == awayTeamQuantileRange
						&& !sampleMatch.isNeutral()) {
					filteredHASample.add(sampleMatch);
					filteredPNSample.add(sampleMatch);
					// System.out.println("HA");
				}
				if (sampleHomeTeamQlRange == awayTeamQuantileRange
						&& sampleAwayTeamQlRange == homeTeamQuantileRange
						&& !sampleMatch.isNeutral()) {
					// System.out.println("RN");
					filteredRNSample.add(sampleMatch);
				}
				if (sampleMatch.isNeutral()) {
					// System.out.println("Neutral");
					if (sampleHomeTeamQlRange == homeTeamQuantileRange
							&& sampleAwayTeamQlRange == awayTeamQuantileRange) {
						filteredNSample.add(sampleMatch);
					}
					if (sampleHomeTeamQlRange == awayTeamQuantileRange
							&& sampleAwayTeamQlRange == homeTeamQuantileRange) {
						filteredNSample.add(sampleMatch);
					}
				}
			}
			
			long finalHomeScore = -1;
			long finalAwayScore = -1;
			
			// Simple case; just use the sample directly
			if (!match.isNeutral()) {

				int simMatchIndex = rnd.nextInt(filteredHASample.size());
				Match simMatch = filteredHASample.get(simMatchIndex);
				
				finalHomeScore = simMatch.getHomeTeamScore();
				finalAwayScore = simMatch.getAwayTeamScore();
				
				match.setHomeTeamScore((int) finalHomeScore);
				match.setAwayTeamScore((int) finalAwayScore);
				
				if (finalHomeScore > -1 && finalAwayScore > -1) {
					break; // We have a valid score. Nothing to do around here
				}

			} else { // Simulate venue neutrality

				int simNeutralMatchIndex = -1;
				int simPNeutralMatchIndex = -1;
				int simRNNeutralMatchIndex = -1;

				int defaultHomeScore = 0;
				int defaultAwayScore = 0;

				try {
					simNeutralMatchIndex = rnd.nextInt(filteredNSample.size());
				} catch (IllegalArgumentException iaex) {
				}

				try {
					simPNeutralMatchIndex = rnd
							.nextInt(filteredPNSample.size());
					simRNNeutralMatchIndex = rnd.nextInt(filteredRNSample
							.size());
				} catch (IllegalArgumentException iaex) {
				}

				Match simPNMatch = null;
				Match simRNMatch = null;

				Match simNMatch = null;

				if (simPNeutralMatchIndex > -1 && simRNNeutralMatchIndex > -1) {
					simPNMatch = filteredPNSample.get(simPNeutralMatchIndex);
					simRNMatch = filteredRNSample.get(simRNNeutralMatchIndex);
				}

				if (simNeutralMatchIndex > -1) {
					simNMatch = filteredNSample.get(simNeutralMatchIndex);
				}

				long simHomeScore = -1;
				long simAwayScore = -1;

				// Default scores
				if (simNMatch != null) {
					defaultHomeScore = simNMatch.getHomeTeamScore();
					defaultHomeScore = simNMatch.getAwayTeamScore();
				} else if (simPNMatch != null) {
					defaultHomeScore = simPNMatch.getHomeTeamScore();
					defaultAwayScore = simPNMatch.getAwayTeamScore();
				} else if (simRNMatch != null) {
					defaultHomeScore = simRNMatch.getHomeTeamScore();
					defaultAwayScore = simRNMatch.getAwayTeamScore();
				} else {
					continue; // Not satisfying sample. Reduce the quantiles.
				}

				if (simPNMatch != null && simRNMatch != null) {
					simHomeScore = Math
							.round((simPNMatch.getHomeTeamScore() + simRNMatch
									.getAwayTeamScore()) / 2d);
					simAwayScore = Math
							.round((simPNMatch.getAwayTeamScore() + simRNMatch
									.getHomeTeamScore()) / 2d);
				}

				if (simHomeScore > -1 && simAwayScore > -1 && simNMatch != null) {
					finalHomeScore = Math
							.round((simNMatch.getHomeTeamScore() + simHomeScore) / 2d);
					finalAwayScore = Math
							.round((simNMatch.getAwayTeamScore() + simAwayScore) / 2d);
				} else if (simNMatch != null) {
					finalHomeScore = simNMatch.getHomeTeamScore();
					finalAwayScore = simNMatch.getAwayTeamScore();
				} else {
					finalHomeScore = defaultHomeScore;
					finalAwayScore = defaultAwayScore;
				}

				match.setHomeTeamScore((int) finalHomeScore);
				match.setAwayTeamScore((int) finalAwayScore);
				
				if (finalHomeScore > -1 && finalAwayScore > -1) {
					break; // We have a valid score. Nothing to do around here
				}
			}
		}
	}
	
	private void debugDisplaySample(List<Match> matches) {
		for (Match match: matches) {
			System.out.println(match);
		}
	}

	private int getTeamQuantile(String teamName, List<List<String>> quantileRangesIndex) {
		for (int i = 0; i < quantileRangesIndex.size(); i++) {
			if (quantileRangesIndex.get(i).contains(teamName)) {
				return i;
			}
		}
		return -1;
	}
}
