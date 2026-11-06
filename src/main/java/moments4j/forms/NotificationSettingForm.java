package moments4j.forms;

/**
 * Port of forms/user.py NotificationSettingForm.
 */
public class NotificationSettingForm {

    private boolean receiveCommentNotification;
    private boolean receiveFollowNotification;
    private boolean receiveCollectNotification;

    public boolean isReceiveCommentNotification() {
        return receiveCommentNotification;
    }

    public void setReceiveCommentNotification(boolean receiveCommentNotification) {
        this.receiveCommentNotification = receiveCommentNotification;
    }

    public boolean isReceiveFollowNotification() {
        return receiveFollowNotification;
    }

    public void setReceiveFollowNotification(boolean receiveFollowNotification) {
        this.receiveFollowNotification = receiveFollowNotification;
    }

    public boolean isReceiveCollectNotification() {
        return receiveCollectNotification;
    }

    public void setReceiveCollectNotification(boolean receiveCollectNotification) {
        this.receiveCollectNotification = receiveCollectNotification;
    }
}
