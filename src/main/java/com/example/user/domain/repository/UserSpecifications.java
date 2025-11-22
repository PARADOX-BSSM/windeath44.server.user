package com.example.user.domain.repository;

import com.example.user.domain.model.User;
import com.example.user.domain.model.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public final class UserSpecifications {
  private UserSpecifications() {
  }

  public static Specification<User> keyword(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return null;
    }

    String lowerKeyword = "%" + keyword.trim().toLowerCase() + "%";
    return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), lowerKeyword),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), lowerKeyword)
    );
  }

  public static Specification<User> hasRole(UserRole role) {
    if (role == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("role"), role);
  }

  public static Specification<User> createdFrom(LocalDateTime createdFrom) {
    if (createdFrom == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
  }

  public static Specification<User> createdTo(LocalDateTime createdTo) {
    if (createdTo == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
  }
}
