package moments4j.forms;

import jakarta.validation.constraints.NotBlank;

/** Port of forms/main.py CommentForm. */
public class CommentForm {

    @NotBlank(message = "This field is required.")
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
