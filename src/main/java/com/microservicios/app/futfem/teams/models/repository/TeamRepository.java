package com.microservicios.app.futfem.teams.models.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.microservicios.app.futfem.teams.models.entity.Team;

public interface TeamRepository extends CrudRepository<Team, Long> {

	@Query("""
			select t from Team t
			where lower(trim(t.name)) like lower(concat('%', ?1, '%'))
			  and lower(trim(coalesce(t.country, ''))) = lower(coalesce(?2, ''))
			order by length(t.name) asc, t.id asc
			""")
	List<Team> findByNameContainingAndCountry(String name, String country);
}
