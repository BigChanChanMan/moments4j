package moments4j.service;

import moments4j.model.Notification;
import moments4j.model.User;
import moments4j.repo.NotificationRepository;
import moments4j.settings.MomentsSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of moments/notifications.py.
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void pushFollowNotification(User follower, User receiver, String userUrl) {
        if (!receiver.isReceiveFollowNotification()) {
            return;
        }
        Notification notification = new Notification();
        notification.setMessage("User <a href=\"" + userUrl + "\">" + follower.getUsername() + "</a> followed you.");
        notification.setReceiver(receiver);
        notifications.save(notification);
    }

    @Transactional
    public void pushCommentNotification(Long photoId, User receiver, int page, String photoUrl) {
        if (!receiver.isReceiveCommentNotification()) {
            return;
        }
        Notification notification = new Notification();
        notification.setMessage("<a href=\"" + photoUrl + "#comments\">This photo</a> has new comment/reply.");
        notification.setReceiver(receiver);
        notifications.save(notification);
    }

    @Transactional
    public void pushCollectNotification(User user, Long photoId, User receiver, String userUrl, String photoUrl) {
        if (!receiver.isReceiveCollectNotification()) {
            return;
        }
        Notification notification = new Notification();
        notification.setMessage("User <a href=\"" + userUrl + "\">" + user.getUsername()
                + "</a> collected your <a href=\"" + photoUrl + "\">photo</a>");
        notification.setReceiver(receiver);
        notifications.save(notification);
    }
}
