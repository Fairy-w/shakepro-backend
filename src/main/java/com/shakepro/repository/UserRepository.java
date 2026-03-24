package com.shakepro.repository;

import com.shakepro.entity.User;
import com.shakepro.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String username, String nickname, Pageable pageable);

    long countByRole(UserRole role);
}
