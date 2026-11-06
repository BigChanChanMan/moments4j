package moments4j.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Port of forms/user.py EditProfileForm. Website URL format and username uniqueness
 * are checked in the controller (mirrors WTForms URL()/custom validators).
 */
public class EditProfileForm {

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 30)
    private String name;

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "The username should contain only a-z, A-Z and 0-9.")
    private String username;

    @Size(max = 255)
    private String website;

    @Size(max = 50)
    private String location;

    @Size(max = 120)
    private String bio;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
