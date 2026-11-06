package moments4j.forms;

import jakarta.validation.constraints.Size;

/** Port of forms/main.py TagForm. */
public class TagForm {

    @Size(max = 64)
    private String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
