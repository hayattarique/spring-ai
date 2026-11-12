package com.ai.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.tool.annotation.ToolParam;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketRequestDto {
    @ToolParam(description = "ticket id if ticket is available otherwise it could be null")
    private Long ticketId;
    @ToolParam(description = "issues of the user")
    private String issues;
    @ToolParam(description = "userAccountId to associate for the user")
    private Long userAccountId;
    @ToolParam(description = "ticket status")
    private String status;
}
