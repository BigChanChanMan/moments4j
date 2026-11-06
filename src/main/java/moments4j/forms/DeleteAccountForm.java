package moments4j.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Port of forms/user.py DeleteAccountForm. */
public class DeleteAccountForm {

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 20)
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
