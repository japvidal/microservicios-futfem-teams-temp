package com.microservicios.app.futfem.teams.services;

import java.util.List;
import java.util.Optional;

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
	public Optional<Team> findByNameContainingAndCountry(String name, String country) {
		String normalizedName = normalize(name);
		String normalizedCountry = normalize(country);

		if (normalizedName == null) {
			return Optional.empty();
		}

		List<Team> teams = repository.findByNameContainingAndCountry(normalizedName, normalizedCountry);
		return teams.stream().findFirst();
	}

	private String normalize(String value) {
		log.debug("Init method TeamServiceImpl.normalize");
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}
}
