package com.example.user.domain.model;

import com.example.user.domain.exception.InsufficientRemainTokenException;
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
    this.remainToken = 10000L;
    String defaultImage = "https://windeath44.s3.ap-northeast-2.amazonaws.com/seori_profile.png";
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

  public void decreaseToken(int tokenCount) {
    boolean canDecreaseRemainToken = this.remainToken >= tokenCount;

    if (!canDecreaseRemainToken) throw InsufficientRemainTokenException.getInstance();

    this.remainToken -= tokenCount;
  }

  public void increaseToken(Long tokenCount) {
    this.remainToken += tokenCount;
  }

  public boolean isAdmin() {
    return this.role.equals(UserRole.ADMIN);
  }

  public void updateRole(UserRole role) {
    this.role = role;
  }
}
