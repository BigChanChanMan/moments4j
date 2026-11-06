package moments4j.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Port of forms/user.py ChangePasswordForm. */
public class ChangePasswordForm {

    @NotBlank(message = "This field is required.")
    private String oldPassword;

    @NotBlank(message = "This field is required.")
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank(message = "This field is required.")
    private String password2;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }
}
