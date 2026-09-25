package com.autodesk.backend.controller;

import com.autodesk.backend.dto.request.AssignTicketRequest;
import com.autodesk.backend.dto.request.CommentRequest;
import com.autodesk.backend.dto.request.StatusUpdateRequest;
import com.autodesk.backend.entity.*;
import com.autodesk.backend.repository.CommentRepository;
import com.autodesk.backend.repository.DepartmentRepository;
import com.autodesk.backend.repository.TicketRepository;
import com.autodesk.backend.repository.UserRepository;
import com.autodesk.backend.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TicketPhase3Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User adminUser;
    private User employeeUser;
    private User otherEmployeeUser;
    private User engineerUser;
    private Department itDepartment;
    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        itDepartment = departmentRepository.save(Department.builder()
                .name("IT Support")
                .description("IT and Tech support")
                .build());

        adminUser = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@autodesk.com")
                .password("password123")
                .role(Role.ROLE_ADMIN)
                .build());

        employeeUser = userRepository.save(User.builder()
                .name("Alice Employee")
                .email("alice@autodesk.com")
                .password("password123")
                .role(Role.ROLE_EMPLOYEE)
                .build());

        otherEmployeeUser = userRepository.save(User.builder()
                .name("Bob Other")
                .email("bob@autodesk.com")
                .password("password123")
                .role(Role.ROLE_EMPLOYEE)
                .build());

        engineerUser = userRepository.save(User.builder()
                .name("Charlie Engineer")
                .email("charlie@autodesk.com")
                .password("password123")
                .role(Role.ROLE_ENGINEER)
                .department(itDepartment)
                .build());

        sampleTicket = ticketRepository.save(Ticket.builder()
                .title("Laptop Screen Flickering")
                .description("Display keeps flickering when plugged in.")
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .category(Category.HARDWARE)
                .createdBy(employeeUser)
                .department(itDepartment)
                .build());
    }

    @Test
    void adminCanAssignEngineerToTicket() throws Exception {
        AssignTicketRequest request = new AssignTicketRequest(engineerUser.getId());

        mockMvc.perform(put("/api/tickets/" + sampleTicket.getId() + "/assign")
                        .with(user(UserDetailsImpl.build(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleTicket.getId()))
                .andExpect(jsonPath("$.assignedTo.id").value(engineerUser.getId()))
                .andExpect(jsonPath("$.assignedTo.email").value("charlie@autodesk.com"));
    }

    @Test
    void adminAssigningNonEngineerReturnsBadRequest() throws Exception {
        // Assigning Alice (who is ROLE_EMPLOYEE, not ROLE_ENGINEER)
        AssignTicketRequest request = new AssignTicketRequest(employeeUser.getId());

        mockMvc.perform(put("/api/tickets/" + sampleTicket.getId() + "/assign")
                        .with(user(UserDetailsImpl.build(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assigned user must have the ROLE_ENGINEER role"));
    }

    @Test
    void nonAdminCannotAssignTicket() throws Exception {
        AssignTicketRequest request = new AssignTicketRequest(engineerUser.getId());

        mockMvc.perform(put("/api/tickets/" + sampleTicket.getId() + "/assign")
                        .with(user(UserDetailsImpl.build(employeeUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatorCanAddCommentAndRetrieveComments() throws Exception {
        CommentRequest commentRequest = new CommentRequest("I tried restarting but the issue persists.");

        mockMvc.perform(post("/api/tickets/" + sampleTicket.getId() + "/comments")
                        .with(user(UserDetailsImpl.build(employeeUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(sampleTicket.getId()))
                .andExpect(jsonPath("$.content").value("I tried restarting but the issue persists."))
                .andExpect(jsonPath("$.user.email").value("alice@autodesk.com"));

        // Retrieve comments
        mockMvc.perform(get("/api/tickets/" + sampleTicket.getId() + "/comments")
                        .with(user(UserDetailsImpl.build(employeeUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("I tried restarting but the issue persists."));
    }

    @Test
    void unauthorizedUserCannotAddCommentOrViewComments() throws Exception {
        CommentRequest commentRequest = new CommentRequest("Can I help?");

        // Bob did not create the ticket and is not assigned/admin
        mockMvc.perform(post("/api/tickets/" + sampleTicket.getId() + "/comments")
                        .with(user(UserDetailsImpl.build(otherEmployeeUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/" + sampleTicket.getId() + "/comments")
                        .with(user(UserDetailsImpl.build(otherEmployeeUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanReopenResolvedTicket() throws Exception {
        sampleTicket.setStatus(TicketStatus.RESOLVED);
        ticketRepository.save(sampleTicket);

        StatusUpdateRequest request = new StatusUpdateRequest(TicketStatus.REOPENED);

        mockMvc.perform(put("/api/tickets/" + sampleTicket.getId() + "/status")
                        .with(user(UserDetailsImpl.build(employeeUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));
    }
}
