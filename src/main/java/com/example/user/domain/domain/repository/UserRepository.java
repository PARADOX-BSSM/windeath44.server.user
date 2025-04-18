package com.example.user.domain.domain.repository;

import com.example.user.domain.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  boolean existsUserByEmail(String email);
}
