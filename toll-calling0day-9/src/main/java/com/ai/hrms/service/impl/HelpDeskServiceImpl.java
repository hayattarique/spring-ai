package com.ai.hrms.service.impl;

import com.ai.hrms.dto.TicketRequestDto;
import com.ai.hrms.entities.HelpDeskEntity;
import com.ai.hrms.repositories.HelpDeskRepositories;
import com.ai.hrms.service.HelpDeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class HelpDeskServiceImpl implements HelpDeskService {

    private final HelpDeskRepositories helpDeskRepositories;

    @Transactional
    @Override
    public TicketRequestDto createQuery(TicketRequestDto requestDto) {
        HelpDeskEntity helpDeskEntity = new HelpDeskEntity();
        helpDeskEntity.setIssues(requestDto.getIssues());
        helpDeskEntity.setStatus(HelpDeskEntity.Status.OPENED);
        helpDeskEntity.setUserAccountId(requestDto.getUserAccountId());

        helpDeskEntity = helpDeskRepositories.save(helpDeskEntity);
        return new
                TicketRequestDto(helpDeskEntity.getId(), helpDeskEntity.getIssues(), helpDeskEntity.getUserAccountId(),
                helpDeskEntity.getStatus().name());
    }

    @Override
    public List<TicketRequestDto> findActiveTicketByUserAccountId(Long id) {
        return helpDeskRepositories.
                findAllByUserAccountIdAndStatusNotIn(id,
                        List.of(HelpDeskEntity.Status.CLOSED, HelpDeskEntity.Status.RESOLVED));
    }


}
