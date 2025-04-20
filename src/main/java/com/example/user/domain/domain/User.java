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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
  @Id
  @Column(name="user_key")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userKey;
  @Column(unique = true, name="user_id")
  private String userId;
  @Column(unique = true)
  private String email;
  private String name;
  private String password;
  @Enumerated(EnumType.STRING)
  private UserRole role;
  private Long remain_token;
  private String profile;
  @CreationTimestamp
  private LocalDateTime created_at;

  @PrePersist
  public void defaultSettings() {
    if (this.remain_token == null) this.remain_token = 0L;
    if (this.profile == null) this.profile = "Default.png";
  }


  public boolean equalsPassword(String password, PasswordEncoder encoder) {
    return encoder.matches(password, this.password);
  }

  public void update(String name, String profile, String password, PasswordEncoder passwordEncoder) {
    this.name = name;
    this.profile = profile;
    changeToEncodedPassword(password, passwordEncoder);
  }

  public void changeToEncodedPassword(String password, PasswordEncoder encoder) {
    String encodedPassword = encoder.encode(password);
    changePassword(encodedPassword);
  }

  private void changePassword(String password) {
    this.password = password;
  }
}
