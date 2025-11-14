package com.nayoung.telemed.role.repo;

import com.nayoung.telemed.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepo extends JpaRepository<Role, Long> {

}
