package com.ai.hrms.tools;

import com.ai.hrms.dto.TicketRequestDto;
import com.ai.hrms.service.HelpDeskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HelpDeskTool {

    private final HelpDeskService deskService;

    @Tool(name = "create_ticket", description = "create a ticket for user issues by passing ",returnDirect = true)
    public TicketRequestDto createTicket(@ToolParam(description = "context which contains userAccountId") ToolContext context, @ToolParam(description = " Issues for the ticket") String issues) {
        log.info("create_ticket -> TOOL CALLED");
        return deskService.createQuery(new TicketRequestDto(null, issues, fetchUserAccountId(context), null));
    }


    @Tool(name = "get_all_tickets", description = "fetch all tickets related to user")

    public List<TicketRequestDto> findAllTickets(@ToolParam(description = "context which contains userAccountId") ToolContext context) {
        log.info("get_all_tickets -> findAllTickets");
        Long userAccountId = fetchUserAccountId(context);
        log.info("user account id: {}", userAccountId);
        return deskService.findActiveTicketByUserAccountId(userAccountId);

    }


    private Long fetchUserAccountId(ToolContext context) {
        Object userAccountId = context.getContext().get("userAccountId");

        if (userAccountId != null) {
            return (Long) userAccountId;
        }

        throw new NullPointerException("userAccountId cannot be null");
    }


}
