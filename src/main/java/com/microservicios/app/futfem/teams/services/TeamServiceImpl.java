package com.microservicios.app.futfem.teams.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microservicios.app.common.services.CommonServiceImpl;
import com.microservicios.app.futfem.teams.models.entity.Team;
import com.microservicios.app.futfem.teams.models.repository.TeamRepository;

@Service
public class TeamServiceImpl extends CommonServiceImpl<Team, TeamRepository> implements TeamService {

	private static final Logger log = LoggerFactory.getLogger(TeamServiceImpl.class);

	@Override
	public Optional<Team> findByNameContainingAndCountry(String name, String nickname, String country) {
		List<String> requestTerms = buildRequestTerms(name, nickname);
		String normalizedCountry = normalize(country);

		if (requestTerms.isEmpty()) {
			return Optional.empty();
		}

		Iterable<Team> candidates = normalizedCountry == null
				? repository.findAll()
				: repository.findByCountryIgnoreCase(normalizedCountry);

		return StreamSupport.stream(candidates.spliterator(), false)
				.map(team -> rankTeam(team, requestTerms))
				.filter(TeamMatch::isCandidate)
				.max(Comparator.comparingInt(TeamMatch::score)
						.thenComparingLong(match -> match.team().getId()))
				.map(TeamMatch::team);
	}

	private List<String> buildRequestTerms(String name, String nickname) {
		log.info("Init method TeamServiceImpl.buildRequestTerms");
		List<String> terms = new ArrayList<>();
		addIfPresent(terms, normalize(name));
		addIfPresent(terms, normalize(nickname));
		return terms;
	}

	private void addIfPresent(List<String> terms, String value) {
		log.info("Init method TeamServiceImpl.addIfPresent");
		if (value != null && !terms.contains(value)) {
			terms.add(value);
		}
	}

	private TeamMatch rankTeam(Team team, List<String> requestTerms) {
		log.info("Init method TeamServiceImpl.rankTeam");
		List<String> teamTerms = buildTeamTerms(team);
		if (teamTerms.isEmpty()) {
			return new TeamMatch(team, false, 0);
		}

		boolean candidate = requestTerms.stream().anyMatch(request ->
				teamTerms.stream().anyMatch(teamTerm ->
						hasPrefixMatch(request, teamTerm, isNicknameTerm(team, teamTerm) ? 2 : 3)));

		if (!candidate) {
			return new TeamMatch(team, false, 0);
		}

		int score = requestTerms.stream()
				.mapToInt(request -> teamTerms.stream()
						.mapToInt(teamTerm -> scoreTerms(request, teamTerm, isNicknameTerm(team, teamTerm) ? 2 : 3))
						.max()
						.orElse(0))
				.sum();

		return new TeamMatch(team, true, score);
	}

	private List<String> buildTeamTerms(Team team) {
		log.info("Init method TeamServiceImpl.buildTeamTerms");
		List<String> terms = new ArrayList<>();
		addIfPresent(terms, normalize(team.getName()));
		addIfPresent(terms, normalize(team.getNickname()));
		return terms;
	}

	private boolean hasPrefixMatch(String left, String right, int minimumPrefixLength) {
		log.info("Init method TeamServiceImpl.hasPrefixMatch");
		return commonPrefixLength(left, right) >= minimumPrefixLength;
	}

	private int scoreTerms(String request, String candidate, int minimumPrefixLength) {
		log.info("Init method TeamServiceImpl.scoreTerms");
		if (request.equals(candidate)) {
			return 100;
		}
		if (request.startsWith(candidate) || candidate.startsWith(request)) {
			return 75;
		}
		int commonPrefix = commonPrefixLength(request, candidate);
		if (commonPrefix >= minimumPrefixLength) {
			return 10 + commonPrefix;
		}
		return 0;
	}

	private boolean isNicknameTerm(Team team, String teamTerm) {
		log.info("Init method TeamServiceImpl.isNicknameTerm");
		String normalizedNickname = normalize(team.getNickname());
		return normalizedNickname != null && normalizedNickname.equals(teamTerm);
	}

	private int commonPrefixLength(String left, String right) {
		log.info("Init method TeamServiceImpl.commonPrefixLength");
		int max = Math.min(left.length(), right.length());
		int count = 0;
		for (int i = 0; i < max; i++) {
			if (left.charAt(i) != right.charAt(i)) {
				break;
			}
			count++;
		}
		return count;
	}

	private String normalize(String value) {
		log.info("Init method TeamServiceImpl.normalize");
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		if (normalized.isEmpty()) {
			return null;
		}
		String sanitized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.replaceAll("[^\\p{Alnum}]", "")
				.toLowerCase();
		return sanitized.isEmpty() ? null : sanitized;
	}

	private record TeamMatch(Team team, boolean candidate, int score) {
		private boolean isCandidate() {
			return candidate;
		}
	}
}
