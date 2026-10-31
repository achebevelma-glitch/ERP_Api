package com.example.ERP_Api.Repository;

import com.example.ERP_Api.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    long countBySeenFalse();

    @Modifying
    @Query("update Notification n set n.seen = true where n.seen = false")
    int markAllSeen();
}
