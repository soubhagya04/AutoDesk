package com.autodesk.backend.service;

import com.autodesk.backend.dto.request.AssignTicketRequest;
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
    private final DepartmentService departmentService;

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
            // If the engineer belongs to a department, include unassigned tickets from that queue.
            if (currentUser.getDepartment() != null) {
                tickets = ticketRepository.findForEngineer(currentUser, currentUser.getDepartment());
            } else {
                tickets = ticketRepository.findByAssignedToOrderByCreatedAtDesc(currentUser);
            }
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

        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            ticket.setStatus(newStatus);
        } else if (currentUser.getRole() == Role.ROLE_ENGINEER) {
            // Engineers may only update status of tickets explicitly assigned to them.
            if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(currentUser.getId())) {
                ticket.setStatus(newStatus);
            } else {
                throw new AccessDeniedException("Engineers can only update the status of tickets assigned to them");
            }
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

    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User engineer = userRepository.findById(request.getEngineerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getEngineerId()));

        if (engineer.getRole() != Role.ROLE_ENGINEER) {
            throw new IllegalArgumentException("Assigned user must have the ROLE_ENGINEER role");
        }

        ticket.setAssignedTo(engineer);
        Ticket updatedTicket = ticketRepository.save(ticket);
        return mapToResponse(updatedTicket);
    }

    public Ticket getTicketEntity(Long id, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        if (!canAccessTicket(ticket, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view this ticket");
        }

        return ticket;
    }

    public boolean canAccessTicket(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) return true;
        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(currentUser.getId())) return true;
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(currentUser.getId())) return true;
        // Engineers can view unassigned tickets belonging to their department queue.
        if (currentUser.getRole() == Role.ROLE_ENGINEER
                && currentUser.getDepartment() != null
                && ticket.getAssignedTo() == null
                && ticket.getDepartment() != null
                && ticket.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
            return true;
        }
        return false;
    }

    public TicketResponse mapToResponse(Ticket ticket) {
        if (ticket == null) return null;

        UserSummaryDto createdByDto = mapUserToSummary(ticket.getCreatedBy());
        UserSummaryDto assignedToDto = mapUserToSummary(ticket.getAssignedTo());
        DepartmentResponse departmentDto = departmentService.mapToResponse(ticket.getDepartment());

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

    public UserSummaryDto mapUserToSummary(User user) {
        if (user == null) return null;
        return UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
