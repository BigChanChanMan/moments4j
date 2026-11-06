package moments4j.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Port of moments/settings.py.
 */
@ConfigurationProperties(prefix = "moments")
public class MomentsSettings {

    private String adminEmail = "admin@helloflask.com";
    private String errorEmailSubject = "[Greybook] Application Error";
    private String loggingPath = "logs/moments.log";
    private int photoPerPage = 12;
    private int commentPerPage = 15;
    private int notificationPerPage = 20;
    private int userPerPage = 20;
    private int managePhotoPerPage = 20;
    private int manageUserPerPage = 30;
    private int manageTagPerPage = 50;
    private int manageCommentPerPage = 30;
    private int searchResultPerPage = 20;
    private String mailSubjectPrefix = "[Moments]";
    private String uploadPath = "uploads";
    private int photoSizeSmall = 400;
    private int photoSizeMedium = 800;
    private int maxFileSizeMb = 3;

    /** Operations from settings.py. */
    public enum Operation {
        CONFIRM("confirm"),
        RESET_PASSWORD("reset-password"),
        CHANGE_EMAIL("change-email");

        private final String value;

        Operation(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getErrorEmailSubject() {
        return errorEmailSubject;
    }

    public void setErrorEmailSubject(String errorEmailSubject) {
        this.errorEmailSubject = errorEmailSubject;
    }

    public String getLoggingPath() {
        return loggingPath;
    }

    public void setLoggingPath(String loggingPath) {
        this.loggingPath = loggingPath;
    }

    public int getPhotoPerPage() {
        return photoPerPage;
    }

    public void setPhotoPerPage(int photoPerPage) {
        this.photoPerPage = photoPerPage;
    }

    public int getCommentPerPage() {
        return commentPerPage;
    }

    public void setCommentPerPage(int commentPerPage) {
        this.commentPerPage = commentPerPage;
    }

    public int getNotificationPerPage() {
        return notificationPerPage;
    }

    public void setNotificationPerPage(int notificationPerPage) {
        this.notificationPerPage = notificationPerPage;
    }

    public int getUserPerPage() {
        return userPerPage;
    }

    public void setUserPerPage(int userPerPage) {
        this.userPerPage = userPerPage;
    }

    public int getManagePhotoPerPage() {
        return managePhotoPerPage;
    }

    public void setManagePhotoPerPage(int managePhotoPerPage) {
        this.managePhotoPerPage = managePhotoPerPage;
    }

    public int getManageUserPerPage() {
        return manageUserPerPage;
    }

    public void setManageUserPerPage(int manageUserPerPage) {
        this.manageUserPerPage = manageUserPerPage;
    }

    public int getManageTagPerPage() {
        return manageTagPerPage;
    }

    public void setManageTagPerPage(int manageTagPerPage) {
        this.manageTagPerPage = manageTagPerPage;
    }

    public int getManageCommentPerPage() {
        return manageCommentPerPage;
    }

    public void setManageCommentPerPage(int manageCommentPerPage) {
        this.manageCommentPerPage = manageCommentPerPage;
    }

    public int getSearchResultPerPage() {
        return searchResultPerPage;
    }

    public void setSearchResultPerPage(int searchResultPerPage) {
        this.searchResultPerPage = searchResultPerPage;
    }

    public String getMailSubjectPrefix() {
        return mailSubjectPrefix;
    }

    public void setMailSubjectPrefix(String mailSubjectPrefix) {
        this.mailSubjectPrefix = mailSubjectPrefix;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public int getPhotoSizeSmall() {
        return photoSizeSmall;
    }

    public void setPhotoSizeSmall(int photoSizeSmall) {
        this.photoSizeSmall = photoSizeSmall;
    }

    public int getPhotoSizeMedium() {
        return photoSizeMedium;
    }

    public void setPhotoSizeMedium(int photoSizeMedium) {
        this.photoSizeMedium = photoSizeMedium;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public String getAvatarsSavePath() {
        return uploadPath + "/avatars";
    }

    /** (small, medium, large) avatar sizes, mirrors AVATARS_SIZE_TUPLE. */
    public int[] getAvatarsSizeTuple() {
        return new int[] {30, 100, 200};
    }
}
