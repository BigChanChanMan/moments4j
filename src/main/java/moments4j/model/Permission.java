package moments4j.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Port of models.py Permission.
 */
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    /** Known permission names (constants used by decorators/templates). */
    public static final String FOLLOW = "FOLLOW";
    public static final String COLLECT = "COLLECT";
    public static final String COMMENT = "COMMENT";
    public static final String UPLOAD = "UPLOAD";
    public static final String MODERATE = "MODERATE";
    public static final String ADMIN = "ADMIN";

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "Permission " + id + ": " + name;
    }
}
