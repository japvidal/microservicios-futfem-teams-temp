package com.microservicios.app.futfem.teams.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.microservicios.app.common.controllers.CommonController;
import com.microservicios.app.futfem.teams.models.entity.Team;
import com.microservicios.app.futfem.teams.services.TeamService;

@RestController
public class TeamController extends CommonController<Team, TeamService> {
	
	@PutMapping("/{id}")
	public ResponseEntity<?> editar(@RequestBody Team team, @PathVariable Long id){
		Optional<Team> o = service.findById(id);
		
		if (!o.isPresent()) {
			return ResponseEntity.notFound().build();
		}
		
		Team teamDb = o.get();
		teamDb.setName(team.getName());
		teamDb.setNickname(team.getNickname());
		teamDb.setCountry(team.getCountry());
		teamDb.setUrlpic(team.getUrlpic());
		teamDb.setUrlshirt(team.getUrlshirt());
		teamDb.setEstablished(team.getEstablished());
		
		// service.save(teamDb) permite persistir el equipo con los datos editados
		return ResponseEntity.status(HttpStatus.CREATED).body(service.save(teamDb));  // HTTPStatus 201
	}
	
}
