package com.nayoung.telemed.role.service;

import com.nayoung.telemed.exceptions.NotFoundException;
import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.role.entity.Role;
import com.nayoung.telemed.role.repo.RoleRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService{

    private final RoleRepo roleRepo;

    @Override
    public Response<Role> createRole(Role roleRequest) {
        Role savedRole = roleRepo.save(roleRequest);

        return success("Role saved successfully", savedRole);
    }

    @Override
    public Response<Role> updateRole(Role roleRequest) {
        Role role = roleRepo.findById(roleRequest.getId())
                .orElseThrow(() -> new NotFoundException("Role not found"));

        role.setName(roleRequest.getName());

        Role updatedRole = roleRepo.save(role);

        return success("Role updated successfully", updatedRole);
    }

    @Override
    public Response<List<Role>> getAllRoles() {
        List<Role> roles = roleRepo.findAll();

        return success("Role retrieved successfully", roles);

    }

    @Override
    public Response<?> deleteRole(Long id) {

        if (!roleRepo.existsById(id)) {
            throw new NotFoundException("Role not found");
        }
        roleRepo.deleteById(id);

        return success("Role deleted successfully", null);
    }

    private <T> Response<T> success(String message, T data) {
        return Response.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }
}
