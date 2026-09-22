package com.autodesk.backend.dto.response;

import com.autodesk.backend.entity.Category;
import com.autodesk.backend.entity.Priority;
import com.autodesk.backend.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private Priority priority;
    private Category category;
    private UserSummaryDto createdBy;
    private UserSummaryDto assignedTo;
    private DepartmentResponse department;
    private String aiCategory;
    private String aiPriority;
    private String aiSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
