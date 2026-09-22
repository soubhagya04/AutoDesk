package com.autodesk.backend.service;

import com.autodesk.backend.dto.request.DepartmentRequest;
import com.autodesk.backend.dto.response.DepartmentResponse;
import com.autodesk.backend.dto.response.MessageResponse;
import com.autodesk.backend.entity.Department;
import com.autodesk.backend.exception.ResourceNotFoundException;
import com.autodesk.backend.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Error: Department with name '" + request.getName() + "' already exists");
        }

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Department savedDepartment = departmentRepository.save(department);
        return mapToResponse(savedDepartment);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return mapToResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (!department.getName().equalsIgnoreCase(request.getName()) &&
                departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Error: Department with name '" + request.getName() + "' already exists");
        }

        department.setName(request.getName());
        department.setDescription(request.getDescription());

        Department updatedDepartment = departmentRepository.save(department);
        return mapToResponse(updatedDepartment);
    }

    @Transactional
    public MessageResponse deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        departmentRepository.delete(department);
        return new MessageResponse("Department deleted successfully");
    }

    public DepartmentResponse mapToResponse(Department department) {
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
