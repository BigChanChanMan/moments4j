package moments4j.forms;

/** Port of forms/user.py PrivacySettingForm. */
public class PrivacySettingForm {

    private boolean publicCollections;

    public boolean isPublicCollections() {
        return publicCollections;
    }

    public void setPublicCollections(boolean publicCollections) {
        this.publicCollections = publicCollections;
    }
}
