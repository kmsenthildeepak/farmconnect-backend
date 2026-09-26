package com.farmconnect.service.impl;

import com.farmconnect.entity.Notification;
import com.farmconnect.entity.User;
import com.farmconnect.repository.NotificationRepository;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl {

    private final NotificationRepository notificationRepository;

    public void notify(User user, String title, String message, Notification.NotificationType type) {
        Notification n = Notification.builder()
                .user(user).title(title).message(message).type(type).isRead(false).build();
        notificationRepository.save(n);
    }

    public List<Notification> myNotifications() {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(SecurityUtil.currentUserId());
    }

    public long unreadCount() {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(SecurityUtil.currentUserId());
    }

    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getUserId().equals(SecurityUtil.currentUserId()))
                .ifPresent(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
    }

    /**
     * Used by the swipe-to-delete gesture. Ownership is checked against the
     * authenticated user (not a client-supplied user id) - a user can only
     * ever delete their own notification.
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getUserId().equals(SecurityUtil.currentUserId()))
                .ifPresent(notificationRepository::delete);
    }
}
