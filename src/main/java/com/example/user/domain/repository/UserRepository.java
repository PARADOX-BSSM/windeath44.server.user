package com.example.user.domain.repository;

import com.example.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  boolean existsUserByEmail(String email);

  boolean existsByUserId(String userId);

  Optional<User> findByUserId(String userId);

  Optional<User> findUserByEmail(String email);
}