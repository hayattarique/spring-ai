package com.ai.hrms.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "help_desk")
@Setter
@Getter
public class HelpDeskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String issues;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPENED;
    @Column(nullable = false)
    private Long userAccountId;


    public static enum Status {
        OPENED, CLOSED, RESOLVED, IN_PROGRESS
    }


}
