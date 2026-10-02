package com.example.flicker.Repository;

import com.example.flicker.model.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findByConversation_IdOrderByCreatedAtAscIdAsc(Long conversationId);
    Optional<DirectMessage> findFirstByConversation_IdOrderByCreatedAtDescIdDesc(Long conversationId);

    @Query("SELECT COUNT(m) FROM DirectMessage m WHERE m.read = false AND m.sender.id <> :userId " +
            "AND (m.conversation.userOne.id = :userId OR m.conversation.userTwo.id = :userId)")
    long countUnreadForUser(@Param("userId") Long userId);


    @Query("SELECT COUNT(m) FROM DirectMessage m WHERE m.conversation.id = :conversationId " +
            "AND m.sender.id <> :userId AND m.read = false")
    long countUnreadFromOtherUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE DirectMessage m SET m.read = true WHERE m.conversation.id = :conversationId " +
            "AND m.sender.id <> :userId AND m.read = false")
    int markIncomingMessagesRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM DirectMessage m WHERE m.conversation.id = :conversationId")
    int deleteConversationMessages(@Param("conversationId") Long conversationId);
}
