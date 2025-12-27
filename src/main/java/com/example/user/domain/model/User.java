package com.example.user.domain.model;

import com.example.user.domain.exception.InsufficientRemainTokenException;
import com.example.user.domain.model.type.LevelTitle;
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
  private static final int MIN_LEVEL = 1;
  private static final int MAX_LEVEL = 44;
  private static final long BASE_LEVEL_UP_XP = 1_000L;
  private static final long[] MIN_XP_FOR_LEVEL = buildMinXpForLevels();

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
  private Long xp;
  @Enumerated(EnumType.STRING)
  private LevelTitle levelTitle;
  private int level;
  @CreatedDate
  private LocalDateTime createdAt;

  @PrePersist
  public void defaultSettings() {
    this.remainToken = 10000L;
    String defaultImage = "https://windeath44.s3.ap-northeast-2.amazonaws.com/seori_profile.png";
    this.profile = defaultImage;
    this.xp = 0L;
    this.level = MIN_LEVEL;
    this.levelTitle = LevelTitle.fromLevel(this.level);
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

  public void updateXp(long newXp) {
    long safeXp = Math.max(newXp, 0);
    this.xp = safeXp;
    this.level = resolveLevelFromXp(safeXp);
    this.levelTitle = LevelTitle.fromLevel(this.level);
  }

  public void applyXpIncrease(Long addedXp, Long totalXp) {
    long currentXp = this.xp == null ? 0L : this.xp;
    long nextXp = resolveNextXp(currentXp, addedXp, totalXp);
    updateXp(nextXp);
  }

  public long getNextLevelRequireXp() {
      if (this.xp == null) {
          return MIN_XP_FOR_LEVEL[2];
      }
      int currentLevel = Math.max(this.level, MIN_LEVEL);
      if (currentLevel >= MAX_LEVEL) {
          return 0;
      }
      long nextLevelMinXp = MIN_XP_FOR_LEVEL[currentLevel + 1];
      return Math.max(nextLevelMinXp - this.xp, 0L);
  }

  private static long resolveNextXp(long currentXp, Long addedXp, Long totalXp) {
    if (totalXp != null) {
      return totalXp;
    }
    if (addedXp != null) {
      return currentXp + addedXp;
    }
    return currentXp;
  }

  private static int resolveLevelFromXp(long xp) {
      /*
             - 레벨업 필요 XP: need(n) = base * r_title(n)^(n-1)
              - 타이틀별 r 예시:
                - MOURNER (Lv 1–9): r = 1.08
              - PATIENT (Lv 10–19): r = 1.10
              - CORPSE (Lv 20–29): r = 1.13
              - GHOST (Lv 30–39): r = 1.16
              - DEMON (Lv 40–44): r = 1.20
       */
    long safeXp = Math.max(xp, 0);
    int left = MIN_LEVEL;
    int right = MAX_LEVEL;
    int result = MIN_LEVEL;

    while (left <= right) {
      int mid = (left + right) / 2;
      long minXp = MIN_XP_FOR_LEVEL[mid];
      if (safeXp >= minXp) {
        result = mid;
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }
    return result;
  }

  private static long[] buildMinXpForLevels() {
    long[] minXpForLevel = new long[MAX_LEVEL + 1];
    minXpForLevel[MIN_LEVEL] = 0L;

    long total = 0L;
    for (int level = MIN_LEVEL; level < MAX_LEVEL; level++) {
      double growthRate = LevelTitle.fromLevel(level).getGrowthRate();
      long needXp = Math.round(BASE_LEVEL_UP_XP * Math.pow(growthRate, level - 1));
      total += needXp;
      minXpForLevel[level + 1] = total;
    }
    return minXpForLevel;
  }
}
