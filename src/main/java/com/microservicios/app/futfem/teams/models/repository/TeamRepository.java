package com.microservicios.app.futfem.teams.models.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.microservicios.app.futfem.teams.models.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

	List<Team> findByCountryIgnoreCase(String country);

	List<Team> findByNameContainingIgnoreCase(String name);

	List<Team> findByNicknameContainingIgnoreCase(String nickname);

	List<Team> findByNameContainingIgnoreCaseAndCountryIgnoreCase(String name, String country);

	List<Team> findByNicknameContainingIgnoreCaseAndCountryIgnoreCase(String nickname, String country);

	@Query("""
			select t from Team t
			where (:search is null
					or lower(coalesce(t.name, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(t.nickname, '')) like lower(concat('%', :search, '%')))
			  and (:country is null or upper(coalesce(t.country, '')) = upper(:country))
			""")
	Page<Team> searchTeams(@Param("search") String search, @Param("country") String country, Pageable pageable);
}
