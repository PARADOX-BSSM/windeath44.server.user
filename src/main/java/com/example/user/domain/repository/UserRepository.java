package com.example.user.domain.repository;

import com.example.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
  boolean existsUserByEmail(String email);

  boolean existsByUserId(String userId);

  Optional<User> findByUserId(String userId);

  Optional<User> findUserByEmail(String email);

  @Query("select u from User u where u.userId in (:userIds)")
  List<User> findByUserIds(List<String> userIds);
}
