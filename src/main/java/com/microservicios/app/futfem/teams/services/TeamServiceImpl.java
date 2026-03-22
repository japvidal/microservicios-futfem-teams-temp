package com.microservicios.app.futfem.teams.services;

import org.springframework.stereotype.Service;

import com.microservicios.app.common.services.CommonServiceImpl;
import com.microservicios.app.futfem.teams.models.entity.Team;
import com.microservicios.app.futfem.teams.models.repository.TeamRepository;

@Service
public class TeamServiceImpl extends CommonServiceImpl<Team, TeamRepository> implements TeamService {


}
