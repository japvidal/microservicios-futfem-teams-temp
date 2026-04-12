package com.microservicios.app.futfem.teams.services;

import java.util.Optional;

import com.microservicios.app.common.services.CommonService;
import com.microservicios.app.futfem.teams.models.entity.Team;
import com.microservicios.app.futfem.teams.services.dto.TeamPageResponse;
import com.microservicios.app.futfem.teams.services.dto.TeamSearchRequest;

public interface TeamService extends CommonService<Team> {

	Optional<Team> findByNameContainingAndCountry(String name, String nickname, String country);

	TeamPageResponse searchTeams(TeamSearchRequest request);
}
