package com.microservicios.app.futfem.teams.models.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.microservicios.app.futfem.teams.models.entity.Team;

public interface TeamRepository extends CrudRepository<Team, Long> {

	List<Team> findByCountryIgnoreCase(String country);
}
