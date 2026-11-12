package com.ai.hrms.service;

import com.ai.hrms.dto.TicketRequestDto;
import com.ai.hrms.entities.HelpDeskEntity;

import java.util.List;

public interface HelpDeskService {

    TicketRequestDto createQuery(TicketRequestDto requestDto);

    List<TicketRequestDto> findActiveTicketByUserAccountId(Long id);
}
