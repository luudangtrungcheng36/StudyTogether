package com.studycommunity.entity;

import com.studycommunity.enums.IdentityProvider;
import com.studycommunity.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_identities")
public class UserIdentity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private IdentityProvider provider = IdentityProvider.DISCORD;

    @Column(name = "provider_subject")
    private String providerSubject;

    @Column(name = "provider_display_name")
    private String providerDisplayName;

    @Column(name = "provider_avatar_url")
    private String providerAvatarUrl;

    @Column(name = "last_authenticated_at")
    private Instant lastAuthenticatedAt;
}
