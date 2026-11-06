package moments4j.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Port of models.py Follow.
 */
@Entity
@Table(name = "follow")
@IdClass(Follow.Pk.class)
public class Follow {

    public static class Pk implements java.io.Serializable {
        public Long follower;
        public Long followed;
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followed_id")
    private User followed;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Follow() {
    }

    public Follow(User follower, User followed) {
        this.follower = follower;
        this.followed = followed;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowed() {
        return followed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Follow: follower_id=" + follower.getId() + ", followed_id=" + followed.getId();
    }
}
