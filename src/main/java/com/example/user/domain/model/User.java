package com.example.user.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
  @Id
  private String userId;
  @Column(unique = true)
  private String email;
  private String name;
  private String password;
  @Enumerated(EnumType.STRING)
  private UserRole role;
  private Long remainToken;
  private String profile;
  @CreatedDate
  private LocalDateTime createdAt;

  @PrePersist
  public void defaultSettings() {
    this.remainToken = 0L;
    String defaultImage = "https://img.freepik.com/premium-vector/default-avatar-profile-icon-social-media-user-image-gray-avatar-icon-blank-profile-silhouette-vector-illustration_561158-3396.jpg?semt=ais_hybrid&w=740";
    this.profile = defaultImage;
  }

  public boolean equalsPassword(String password, PasswordEncoder encoder) {
    return encoder.matches(password, this.password);
  }

  public void changeToEncodedPassword(String password, PasswordEncoder encoder) {
    String encodedPassword = encoder.encode(password);
    changePassword(encodedPassword);
  }

  private void changePassword(String password) {
    this.password = password;
  }

  public void updateProfile(String profile) {
    this.profile = profile;
  }

  public void updateName(String name) {
    this.name = name;
  }
}