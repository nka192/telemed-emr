package com.nayoung.telemed.notification.service;

import com.nayoung.telemed.notification.dto.NotificationDTO;
import com.nayoung.telemed.users.entity.User;

public interface NotificationService {
    void sendEmail(NotificationDTO notificationDTO, User user);
}
