package com.nayoung.telemed.users.repo;

import com.nayoung.telemed.users.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepo extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findByCode(String code);

    void deleteByUserId(Long userId);
}
