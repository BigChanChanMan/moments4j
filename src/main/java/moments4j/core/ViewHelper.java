package moments4j.core;

import moments4j.model.Photo;
import moments4j.model.Tag;
import moments4j.model.User;
import moments4j.service.UserService;
import org.springframework.stereotype.Component;

/**
 * Short-named SpEL helper for templates, replacing the computed properties on
 * models.py entities (photo.collectors_count, user.photos_count, current_user.can(...)).
 * Usage in Thymeleaf: ${v.collectors(photo)}, ${v.can(currentUser, 'MODERATE')}.
 */
@Component("v")
public class ViewHelper {

    private final UserService users;

    public ViewHelper(UserService users) {
        this.users = users;
    }

    public long collectors(Photo photo) {
        return users.photoCollectorsCount(photo);
    }

    public long comments(Photo photo) {
        return users.photoCommentsCount(photo);
    }

    public long photos(User user) {
        return users.photosCount(user);
    }

    public long followers(User user) {
        return users.followersCount(user);
    }

    public long followingCount(User user) {
        return users.followingCount(user);
    }

    public long collections(User user) {
        return users.collectionsCount(user);
    }

    public long tagPhotos(Tag tag) {
        return tag.getPhotos().size();
    }

    public boolean following(User follower, User followed) {
        return follower != null && followed != null && users.isFollowing(follower, followed);
    }

    public boolean followedBy(User user, User other) {
        return user != null && other != null && users.isFollowedBy(user, other);
    }

    public boolean collecting(User user, Photo photo) {
        return user != null && photo != null && users.isCollecting(user, photo);
    }

    public boolean can(User user, String permission) {
        return users.can(user, permission);
    }

    public boolean isAdmin(User user) {
        return user != null && user.isAdmin();
    }
}
