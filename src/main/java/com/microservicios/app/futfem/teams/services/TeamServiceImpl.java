package com.microservicios.app.futfem.teams.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microservicios.app.common.services.CommonServiceImpl;
import com.microservicios.app.futfem.teams.models.entity.Team;
import com.microservicios.app.futfem.teams.models.repository.TeamRepository;
import com.microservicios.app.futfem.teams.services.dto.TeamPageResponse;
import com.microservicios.app.futfem.teams.services.dto.TeamSearchRequest;

@Service
public class TeamServiceImpl extends CommonServiceImpl<Team, TeamRepository> implements TeamService {

	private static final Logger log = LoggerFactory.getLogger(TeamServiceImpl.class);
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 25;

	@Override
	public Optional<Team> findByNameContainingAndCountry(String name, String nickname, String country) {
		String searchName = normalizeSearchValue(name);
		String searchNickname = normalizeSearchValue(nickname);
		String normalizedCountry = normalize(country);

		if (searchName == null && searchNickname == null) {
			return Optional.empty();
		}

		List<Team> candidates = findCandidates(searchName, searchNickname, country);
		if (candidates.isEmpty()) {
			Iterable<Team> fallbackCandidates = normalizedCountry == null
					? repository.findAll()
					: repository.findByCountryIgnoreCase(normalizedCountry);

			candidates = StreamSupport.stream(fallbackCandidates.spliterator(), false).toList();
		}

		return candidates.stream()
				.map(team -> rankTeam(team, searchName, searchNickname))
				.filter(TeamMatch::isCandidate)
				.max(Comparator.comparingInt(TeamMatch::score)
						.thenComparingLong(match -> match.team().getId()))
				.map(TeamMatch::team);
	}

	@Override
	public TeamPageResponse searchTeams(TeamSearchRequest request) {
		log.info("Init method TeamServiceImpl.searchTeams");
		TeamSearchRequest safeRequest = request == null ? new TeamSearchRequest() : request;
		int page = safeRequest.getPage() == null || safeRequest.getPage() < 0 ? DEFAULT_PAGE : safeRequest.getPage();
		int size = safeRequest.getSize() == null || safeRequest.getSize() <= 0 ? DEFAULT_SIZE : safeRequest.getSize();
		Pageable pageable = PageRequest.of(page, size,
				Sort.by("name").ascending().and(Sort.by("nickname").ascending()));

		Page<Team> result = repository.searchTeams(
				normalizeSearchValue(safeRequest.getSearch()),
				normalizeCountryCode(safeRequest.getCountry()),
				pageable);

		return new TeamPageResponse(result.getContent(), result.getTotalElements(), result.getTotalPages(),
				result.getNumber(), result.getSize());
	}

	private List<Team> findCandidates(String name, String nickname, String country) {
		log.info("Init method TeamServiceImpl.findCandidates");
		Map<Long, Team> candidates = new LinkedHashMap<>();
		addCandidates(candidates, findByNameOrCountry(name, country));
		addCandidates(candidates, findByNicknameOrCountry(nickname, country));
		return new ArrayList<>(candidates.values());
	}

	private List<Team> findByNameOrCountry(String name, String country) {
		log.info("Init method TeamServiceImpl.findByNameOrCountry");
		if (name == null) {
			return List.of();
		}
		return country == null || country.trim().isEmpty()
				? repository.findByNameContainingIgnoreCase(name)
				: repository.findByNameContainingIgnoreCaseAndCountryIgnoreCase(name, country.trim());
	}

	private List<Team> findByNicknameOrCountry(String nickname, String country) {
		log.info("Init method TeamServiceImpl.findByNicknameOrCountry");
		if (nickname == null) {
			return List.of();
		}
		return country == null || country.trim().isEmpty()
				? repository.findByNicknameContainingIgnoreCase(nickname)
				: repository.findByNicknameContainingIgnoreCaseAndCountryIgnoreCase(nickname, country.trim());
	}

	private void addCandidates(Map<Long, Team> candidates, List<Team> teams) {
		log.info("Init method TeamServiceImpl.addCandidates");
		for (Team team : teams) {
			candidates.putIfAbsent(team.getId(), team);
		}
	}

	private TeamMatch rankTeam(Team team, String requestName, String requestNickname) {
		log.info("Init method TeamServiceImpl.rankTeam");
		String teamName = normalizeSearchValue(team.getName());
		String teamNickname = normalizeSearchValue(team.getNickname());
		if (teamName == null && teamNickname == null) {
			return new TeamMatch(team, false, 0);
		}

		int officialNameScore = scoreOfficialName(requestName, teamName);
		int nicknameScore = scoreNickname(requestName, requestNickname, teamNickname);
		boolean candidate = officialNameScore > 0 || nicknameScore > 0;

		if (!candidate) {
			return new TeamMatch(team, false, 0);
		}

		int score = officialNameScore + nicknameScore;

		return new TeamMatch(team, true, score);
	}

	private int scoreOfficialName(String requestName, String teamName) {
		log.info("Init method TeamServiceImpl.scoreOfficialName");
		if (requestName == null || teamName == null) {
			return 0;
		}

		String normalizedRequestName = normalize(requestName);
		String normalizedTeamName = normalize(teamName);
		if (normalizedRequestName == null || normalizedTeamName == null) {
			return 0;
		}

		if (normalizedRequestName.equals(normalizedTeamName)) {
			return 220;
		}

		List<String> requestTokens = tokenize(requestName);
		List<String> teamTokens = tokenize(teamName);
		if (requestTokens.isEmpty() || teamTokens.isEmpty()) {
			return 0;
		}

		int firstTokenScore = scoreTokenMatch(requestTokens.get(0), teamTokens.get(0), 3);
		if (firstTokenScore == 0) {
			return 0;
		}

		int secondTokenScore = 0;
		if (requestTokens.size() >= 2) {
			String requestSecondToken = requestTokens.get(1);
			secondTokenScore = teamTokens.stream()
					.skip(1)
					.mapToInt(teamToken -> scoreTokenMatch(requestSecondToken, teamToken, 3))
					.max()
					.orElse(0);
			if (secondTokenScore == 0) {
				return 0;
			}
		}

		return 80 + firstTokenScore + secondTokenScore;
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

	private int scoreNickname(String requestName, String requestNickname, String teamNickname) {
		log.info("Init method TeamServiceImpl.scoreNickname");
		String normalizedTeamNickname = normalize(teamNickname);
		if (normalizedTeamNickname == null) {
			return 0;
		}

		int score = 0;
		score = Math.max(score, scoreNormalizedValues(requestNickname, teamNickname, 2));
		score = Math.max(score, scoreNormalizedValues(requestName, teamNickname, 2));

		for (String requestToken : tokenize(requestName)) {
			score = Math.max(score, scoreTokenMatch(requestToken, normalizedTeamNickname, 2));
		}
		for (String requestToken : tokenize(requestNickname)) {
			score = Math.max(score, scoreTokenMatch(requestToken, normalizedTeamNickname, 2));
		}

		return score == 0 ? 0 : 140 + score;
	}

	private int scoreNormalizedValues(String left, String right, int minimumPrefixLength) {
		log.info("Init method TeamServiceImpl.scoreNormalizedValues");
		String normalizedLeft = normalize(left);
		String normalizedRight = normalize(right);
		if (normalizedLeft == null || normalizedRight == null) {
			return 0;
		}
		return scoreTokenMatch(normalizedLeft, normalizedRight, minimumPrefixLength);
	}

	private int scoreTokenMatch(String left, String right, int minimumPrefixLength) {
		log.info("Init method TeamServiceImpl.scoreTokenMatch");
		if (left == null || right == null) {
			return 0;
		}
		if (left.equals(right)) {
			return 100;
		}
		if (left.contains(right) || right.contains(left)) {
			return 80;
		}
		int commonPrefix = commonPrefixLength(left, right);
		if (commonPrefix >= minimumPrefixLength) {
			return 20 + commonPrefix;
		}
		return 0;
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
				.toLowerCase(Locale.ROOT);
		return sanitized.isEmpty() ? null : sanitized;
	}

	private String normalizeSearchValue(String value) {
		log.info("Init method TeamServiceImpl.normalizeSearchValue");
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}

	private String normalizeCountryCode(String value) {
		log.info("Init method TeamServiceImpl.normalizeCountryCode");
		String normalized = normalizeSearchValue(value);
		return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
	}

	private List<String> tokenize(String value) {
		log.info("Init method TeamServiceImpl.tokenize");
		if (value == null) {
			return List.of();
		}

		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);

		List<String> tokens = new ArrayList<>();
		for (String part : normalized.split("[^\\p{Alnum}]+")) {
			String token = normalize(part);
			if (token != null && !tokens.contains(token)) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	private record TeamMatch(Team team, boolean candidate, int score) {
		private boolean isCandidate() {
			return candidate;
		}
	}
}
