package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByLastNameAscFirstNameAsc();

    List<User> findAllByRoleOrderByLastNameAscFirstNameAsc(Role role);

    long countByRoleAndActiveTrue(Role role);
}
