package com.ai.hrms.repositories;

import com.ai.hrms.dto.TicketRequestDto;
import com.ai.hrms.entities.HelpDeskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HelpDeskRepositories extends JpaRepository<HelpDeskEntity, Long> {

    @Query("""
            SELECT new com.ai.hrms.dto.TicketRequestDto(
                h.id,
                h.issues,
                h.userAccountId,
                CAST(h.status AS string)
            )
            FROM HelpDeskEntity h
            WHERE h.userAccountId = :userAccountId
            AND h.status NOT IN :statuses
            """)
    List<TicketRequestDto> findAllByUserAccountIdAndStatusNotIn(
            @Param("userAccountId") Long userAccountId,
            @Param("statuses") List<HelpDeskEntity.Status> statuses
    );
}
