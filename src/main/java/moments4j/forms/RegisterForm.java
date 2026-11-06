package moments4j.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Port of forms/auth.py RegisterForm. Uniqueness and EqualTo('password2') checks
 * run in the controller against BindingResult (mirrors WTForms validators).
 */
public class RegisterForm {

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 30)
    private String name;

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 254)
    private String email;

    @NotBlank(message = "This field is required.")
    @Size(min = 1, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "The username should contain only a-z, A-Z and 0-9.")
    private String username;

    @NotBlank(message = "This field is required.")
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank(message = "This field is required.")
    private String password2;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
