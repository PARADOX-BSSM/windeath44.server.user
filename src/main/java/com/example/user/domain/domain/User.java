package com.example.user.domain.domain;

import com.example.user.domain.presentation.dto.request.UserUpdateRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @Column(name="user_id")
  private String userId;
  private String email;
  private String name;
  private String password;
  @Enumerated(EnumType.STRING)
  private Role role;
  private Long remain_token = 0L;
  private String profile = "default.png";
  @CreationTimestamp
  private LocalDateTime created_at;

  @PrePersist
  public void defaultSettings() {
    if (this.remain_token == null) this.remain_token = 0L;
    if (this.profile == null) this.profile = "Default.png";
  }

  public static User create(String userId, String email, String name) {
    return User.builder()
            .userId(userId)
            .email(email)
            .name(name)
            .role(Role.USER)
            .build();
  }
  public boolean equalsPassword(String password, PasswordEncoder encoder) {
    return encoder.matches(password, this.password);
  }

  public void update(UserUpdateRequest updateInfo, PasswordEncoder passwordEncoder) {
    this.name = updateInfo.name();
    this.profile = updateInfo.profile();
    changeToEncodedPassword(updateInfo.password(), passwordEncoder);
  }

  public void changeToEncodedPassword(String password, PasswordEncoder encoder) {
    String encodedPassword = encoder.encode(password);
    changePassword(encodedPassword);
  }

  private void changePassword(String password) {
    this.password = password;
  }
}
