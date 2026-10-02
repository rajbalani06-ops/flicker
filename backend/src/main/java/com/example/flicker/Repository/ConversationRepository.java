package com.example.flicker.Repository;

import com.example.flicker.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.userOne.id = :firstId AND c.userTwo.id = :secondId) OR " +
            "(c.userOne.id = :secondId AND c.userTwo.id = :firstId)")
    Optional<Conversation> findBetweenUsers(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

    List<Conversation> findByUserOne_IdOrUserTwo_IdOrderByUpdatedAtDesc(Long userOneId, Long userTwoId);
}
