package com.microservicios.app.futfem.teams.services.dto;

import java.util.List;

import com.microservicios.app.futfem.teams.models.entity.Team;

public record TeamPageResponse(
		List<Team> content,
		long totalElements,
		int totalPages,
		int page,
		int size) {
}
