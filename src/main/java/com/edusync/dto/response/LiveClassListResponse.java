package com.edusync.dto.response;

import java.util.List;

/** Lista de aulas do professor, separada em próximas e finalizadas. */
public record LiveClassListResponse(
        List<LiveClassResponseDTO> upcoming,
        List<LiveClassResponseDTO> finished
) {}
