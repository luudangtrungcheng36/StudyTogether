package com.studycommunity.repository;

import com.studycommunity.entity.UserIdentity;
import com.studycommunity.enums.IdentityProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<UserIdentity> findByProviderAndProviderSubject(
            IdentityProvider provider, String providerSubject);
}
