package moments4j.forms;

import jakarta.validation.constraints.Size;

/** Port of forms/main.py DescriptionForm. */
public class DescriptionForm {

    @Size(max = 500)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
