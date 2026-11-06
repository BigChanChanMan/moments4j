package moments4j.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Port of forms/user.py ChangeEmailForm. */
public class ChangeEmailForm {

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 254)
    @Email
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
