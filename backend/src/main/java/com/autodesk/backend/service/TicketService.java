package com.autodesk.backend.service;

import com.autodesk.backend.dto.request.StatusUpdateRequest;
import com.autodesk.backend.dto.request.TicketRequest;
import com.autodesk.backend.dto.response.DepartmentResponse;
import com.autodesk.backend.dto.response.TicketResponse;
import com.autodesk.backend.dto.response.UserSummaryDto;
import com.autodesk.backend.entity.*;
import com.autodesk.backend.exception.ResourceNotFoundException;
import com.autodesk.backend.repository.DepartmentRepository;
import com.autodesk.backend.repository.TicketRepository;
import com.autodesk.backend.repository.UserRepository;
import com.autodesk.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public User getUserFromDetails(UserDetailsImpl userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));
    }

    @Transactional
    public TicketResponse createTicket(TicketRequest request, User currentUser) {
        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : Category.OTHER)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .status(TicketStatus.OPEN)
                .createdBy(currentUser)
                .department(department)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToResponse(savedTicket);
    }

    public List<TicketResponse> getAllTickets(User currentUser) {
        List<Ticket> tickets;

        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        } else if (currentUser.getRole() == Role.ROLE_ENGINEER) {
            tickets = ticketRepository.findByAssignedToOrderByCreatedAtDesc(currentUser);
        } else {
            tickets = ticketRepository.findByCreatedByOrderByCreatedAtDesc(currentUser);
        }

        return tickets.stream().map(this::mapToResponse).toList();
    }

    public TicketResponse getTicketById(Long id, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        if (!canAccessTicket(ticket, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view this ticket");
        }

        return mapToResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long id, StatusUpdateRequest request, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        TicketStatus newStatus = request.getStatus();

        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_ENGINEER) {
            ticket.setStatus(newStatus);
        } else if (currentUser.getRole() == Role.ROLE_EMPLOYEE) {
            if (ticket.getCreatedBy().getId().equals(currentUser.getId())) {
                if (newStatus == TicketStatus.REOPENED &&
                        (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED)) {
                    ticket.setStatus(TicketStatus.REOPENED);
                } else {
                    throw new AccessDeniedException("Employees can only transition resolved/closed tickets to REOPENED");
                }
            } else {
                throw new AccessDeniedException("You can only modify status on tickets created by you");
            }
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        return mapToResponse(updatedTicket);
    }

    private boolean canAccessTicket(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return true;
        }
        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            return true;
        }
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(currentUser.getId())) {
            return true;
        }
        return currentUser.getRole() == Role.ROLE_ENGINEER;
    }

    public TicketResponse mapToResponse(Ticket ticket) {
        if (ticket == null) return null;

        UserSummaryDto createdByDto = mapUserToSummary(ticket.getCreatedBy());
        UserSummaryDto assignedToDto = mapUserToSummary(ticket.getAssignedTo());
        DepartmentResponse departmentDto = mapDepartmentToResponse(ticket.getDepartment());

        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .createdBy(createdByDto)
                .assignedTo(assignedToDto)
                .department(departmentDto)
                .aiCategory(ticket.getAiCategory())
                .aiPriority(ticket.getAiPriority())
                .aiSummary(ticket.getAiSummary())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private UserSummaryDto mapUserToSummary(User user) {
        if (user == null) return null;
        return UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private DepartmentResponse mapDepartmentToResponse(Department department) {
        if (department == null) return null;
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
