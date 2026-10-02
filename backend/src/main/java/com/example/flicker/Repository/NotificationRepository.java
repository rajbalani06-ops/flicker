package com.example.flicker.Repository;


import com.example.flicker.model.Notification;
import com.example.flicker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.targetId = :postId AND n.type IN :types")
    int deletePostNotifications(@Param("postId") Long postId, @Param("types") List<String> types);
}
