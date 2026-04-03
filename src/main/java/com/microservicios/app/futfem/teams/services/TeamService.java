package com.microservicios.app.futfem.teams.services;

import java.util.Optional;

import com.microservicios.app.common.services.CommonService;
import com.microservicios.app.futfem.teams.models.entity.Team;

public interface TeamService extends CommonService<Team> {

	Optional<Team> findByNameContainingAndCountry(String name, String nickname, String country);
}
