package com.example.user.domain.domain.repository;

import com.example.user.domain.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.OptionalLong;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  boolean existsUserByEmail(String email);

  boolean existsByUserId(String userId);

  Optional<User> findByUserId(String userId);

  Optional<User> findUserByEmail(String email);
}
