package moments4j.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Port of models.py Collection.
 */
@Entity
@Table(name = "collection")
@IdClass(Collection.Pk.class)
public class Collection {

    public static class Pk implements java.io.Serializable {
        public Long user;
        public Long photo;
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id")
    private Photo photo;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Collection() {
    }

    public Collection(User user, Photo photo) {
        this.user = user;
        this.photo = photo;
    }

    public User getUser() {
        return user;
    }

    public Photo getPhoto() {
        return photo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Collect: user_id=" + user.getId() + ", photo_id=" + photo.getId();
    }
}
