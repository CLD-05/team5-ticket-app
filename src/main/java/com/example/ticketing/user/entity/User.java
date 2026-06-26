package com.example.ticketing.user.entity;

import com.example.ticketing.user.domain.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.userId == null) {
            this.userId = java.util.UUID.randomUUID().toString();
        }

        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.provider == null) {
            this.provider = AuthProvider.LOCAL;
        }
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public void updateProvider(AuthProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }
}