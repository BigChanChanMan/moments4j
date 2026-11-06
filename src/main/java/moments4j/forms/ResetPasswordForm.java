package moments4j.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Port of forms/auth.py ResetPasswordForm. */
public class ResetPasswordForm {

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 254)
    @Email
    private String email;

    @NotBlank(message = "This field is required.")
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank(message = "This field is required.")
    private String password2;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
