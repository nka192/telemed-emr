package com.nayoung.telemed.notification.repo;

import com.nayoung.telemed.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

}
