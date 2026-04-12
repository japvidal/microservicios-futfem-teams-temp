package com.microservicios.app.futfem.teams.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.microservicios.app.common.controllers.CommonController;
import com.microservicios.app.futfem.teams.controllers.dto.TeamLookupRequest;
import com.microservicios.app.futfem.teams.models.entity.Team;
import com.microservicios.app.futfem.teams.services.TeamService;
import com.microservicios.app.futfem.teams.services.dto.TeamSearchRequest;

@RestController
public class TeamController extends CommonController<Team, TeamService> {

	@PostMapping("/getIdByName")
	public ResponseEntity<?> getIdByName(@RequestBody TeamLookupRequest request) {
		Optional<Team> team = service.findByNameContainingAndCountry(
			request.getName(),
			request.getNickname(),
			request.getCountry()
		);

		if (team.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(team.get().getId());
	}

	@PostMapping("/search")
	public ResponseEntity<?> search(@RequestBody(required = false) TeamSearchRequest request) {
		return ResponseEntity.ok(service.searchTeams(request == null ? new TeamSearchRequest() : request));
	}

	@PostMapping("/")
	public ResponseEntity<?> crear(@RequestBody Team team) {
		Optional<Team> existing = service.findByNameContainingAndCountry(
			team.getName(),
			team.getNickname(),
			team.getCountry()
		);

		if (existing.isPresent()) {
			Team teamDb = existing.get();
			boolean sameName = teamDb.getName() != null && teamDb.getName().equalsIgnoreCase(team.getName());
			boolean sameNickname = (teamDb.getNickname() == null && team.getNickname() == null)
					|| (teamDb.getNickname() != null && team.getNickname() != null
							&& teamDb.getNickname().equalsIgnoreCase(team.getNickname()));
			boolean sameCountry = (teamDb.getCountry() == null && team.getCountry() == null)
					|| (teamDb.getCountry() != null && team.getCountry() != null
							&& teamDb.getCountry().equalsIgnoreCase(team.getCountry()));

			if (sameName && sameNickname && sameCountry) {
				return ResponseEntity.ok(teamDb);
			}
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(service.save(team));
	}
	
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
