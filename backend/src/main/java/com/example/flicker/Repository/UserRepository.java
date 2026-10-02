package com.example.flicker.Repository;


import com.example.flicker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE u.id <> :currentUserId AND (" +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY LOWER(COALESCE(u.displayName, u.username)), LOWER(u.username)")
    java.util.List<User> searchUsers(@Param("query") String query,
                                     @Param("currentUserId") Long currentUserId,
                                     Pageable pageable);
}
