package moments4j.forms;

import jakarta.validation.constraints.NotBlank;

/**
 * Port of forms/admin.py EditProfileAdminForm. Email/username uniqueness and
 * role choice are validated in the controller.
 */
public class EditProfileAdminForm {

    @NotBlank(message = "This field is required.")
    private String name;

    @NotBlank(message = "This field is required.")
    private String email;

    private Long role;

    private boolean active;

    private boolean confirmed;

    @NotBlank(message = "This field is required.")
    private String username;

    private String bio;

    private String website;

    private String location;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getRole() {
        return role;
    }

    public void setRole(Long role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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
}
