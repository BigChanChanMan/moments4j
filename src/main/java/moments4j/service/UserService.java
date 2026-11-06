package moments4j.service;

import moments4j.model.*;
import moments4j.repo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Port of the behavior methods on models.py User, plus Role.init_role.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final FollowRepository follows;
    private final CollectionRepository collections;
    private final PhotoRepository photos;
    private final NotificationRepository notifications;
    private final CommentRepository comments;
    private final PasswordEncoder encoder;
    private final AvatarService avatars;
    private final moments4j.settings.MomentsSettings settings;

    public UserService(UserRepository users, RoleRepository roles, PermissionRepository permissions,
                       FollowRepository follows, CollectionRepository collections, PhotoRepository photos,
                       NotificationRepository notifications, CommentRepository comments,
                       PasswordEncoder encoder, AvatarService avatars, moments4j.settings.MomentsSettings settings) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.follows = follows;
        this.collections = collections;
        this.photos = photos;
        this.notifications = notifications;
        this.comments = comments;
        this.encoder = encoder;
        this.avatars = avatars;
        this.settings = settings;
    }

    // ---- Role.init_role ----

    @Transactional
    public void initRole() {
        var permissionsByRole = new java.util.LinkedHashMap<String, String[]>();
        permissionsByRole.put("Locked", new String[] {"FOLLOW", "COLLECT"});
        permissionsByRole.put("User", new String[] {"FOLLOW", "COLLECT", "COMMENT", "UPLOAD"});
        permissionsByRole.put("Moderator", new String[] {"FOLLOW", "COLLECT", "COMMENT", "UPLOAD", "MODERATE"});
        permissionsByRole.put("Administrator",
                new String[] {"FOLLOW", "COLLECT", "COMMENT", "UPLOAD", "MODERATE", "ADMIN"});

        for (var entry : permissionsByRole.entrySet()) {
            Role role = roles.findByName(entry.getKey()).orElseGet(() -> {
                Role r = new Role();
                r.setName(entry.getKey());
                return roles.save(r);
            });
            role.getPermissions().clear();
            for (String permissionName : entry.getValue()) {
                Permission permission = permissions.findByName(permissionName).orElseGet(() -> {
                    Permission p = new Permission();
                    p.setName(permissionName);
                    return permissions.save(p);
                });
                role.getPermissions().add(permission);
            }
            roles.save(role);
        }
    }

    // ---- user creation (models.py User.__init__) ----

    @Transactional
    public User createUser(String name, String email, String username, String password, boolean confirmed) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setConfirmed(confirmed);
        avatars.generateIdenticon(user);   // generate_avatar()
        user = users.save(user);
        followInternal(user, user);        // follow self
        setRole(user);                     // set_role()
        return users.save(user);
    }

    private void setRole(User user) {
        if (user.getRole() == null) {
            String roleName = user.getEmail().equalsIgnoreCase(settings.getAdminEmail())
                    ? "Administrator" : "User";
            user.setRole(roles.findByName(roleName).orElse(null));
        }
    }

    // ---- password ----

    public boolean validatePassword(User user, String password) {
        return encoder.matches(password, user.getPasswordHash());
    }

    public void setPassword(User user, String password) {
        user.setPasswordHash(encoder.encode(password));
    }

    // ---- follow ----

    @Transactional
    public void follow(User follower, User followed) {
        followInternal(follower, followed);
    }

    private void followInternal(User follower, User followed) {
        if (!isFollowing(follower, followed)) {
            follows.save(new Follow(follower, followed));
        }
    }

    @Transactional
    public void unfollow(User follower, User followed) {
        follows.findByFollowerIdAndFollowedId(follower.getId(), followed.getId())
                .ifPresent(follows::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(User follower, User followed) {
        if (followed.getId() == null) {  // user.id will be null when follow self
            return false;
        }
        return follows.findByFollowerIdAndFollowedId(follower.getId(), followed.getId()).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isFollowedBy(User user, User other) {
        return follows.findByFollowerIdAndFollowedId(other.getId(), user.getId()).isPresent();
    }

    // ---- collect ----

    @Transactional
    public void collect(User user, Photo photo) {
        if (!isCollecting(user, photo)) {
            collections.save(new Collection(user, photo));
        }
    }

    @Transactional
    public void uncollect(User user, Photo photo) {
        collections.findByUserIdAndPhotoId(user.getId(), photo.getId())
                .ifPresent(collections::delete);
    }

    @Transactional(readOnly = true)
    public boolean isCollecting(User user, Photo photo) {
        return collections.findByUserIdAndPhotoId(user.getId(), photo.getId()).isPresent();
    }

    // ---- lock / block (admin.py uses these) ----

    @Transactional
    public void lock(User user) {
        user.setLocked(true);
        roles.findByName("Locked").ifPresent(user::setRole);
    }

    @Transactional
    public void unlock(User user) {
        user.setLocked(false);
        roles.findByName("User").ifPresent(user::setRole);
    }

    @Transactional
    public void block(User user) {
        user.setActive(false);
    }

    @Transactional
    public void unblock(User user) {
        user.setActive(true);
    }

    // ---- permission check (User.can / Guest.can) ----

    @Transactional(readOnly = true)
    public boolean can(User user, String permissionName) {
        if (user == null || user.getId() == null) {
            return false;  // Guest
        }
        // re-load inside the transaction: the caller's entity may be detached
        // (e.g. called from AuthInterceptor before the OSIV session is open)
        User managed = users.findById(user.getId()).orElse(null);
        if (managed == null || managed.getRole() == null) {
            return false;
        }
        return managed.getRole().getPermissions().stream()
                .anyMatch(p -> p.getName().equals(permissionName));
    }

    // ---- counts (User properties) ----

    @Transactional(readOnly = true)
    public long followersCount(User user) {
        return follows.countByFollowedId(user.getId()) - 1;  // minus user self
    }

    @Transactional(readOnly = true)
    public long followingCount(User user) {
        return follows.countByFollowerId(user.getId()) - 1;  // minus user self
    }

    @Transactional(readOnly = true)
    public long photosCount(User user) {
        return photos.countByAuthorId(user.getId());
    }

    @Transactional(readOnly = true)
    public long collectionsCount(User user) {
        return collections.countByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public long notificationsCount(User user) {
        return notifications.countByReceiverIdAndReadFalse(user.getId());
    }

    @Transactional(readOnly = true)
    public long photoCommentsCount(Photo photo) {
        return comments.countByPhotoId(photo.getId());
    }

    @Transactional(readOnly = true)
    public long photoCollectorsCount(Photo photo) {
        return collections.countByPhotoId(photo.getId());
    }

    @Transactional(readOnly = true)
    public long tagPhotosCount(Tag tag) {
        return tag.getPhotos().size();
    }

    // ---- paged listings ----

    @Transactional(readOnly = true)
    public Page<Photo> userPhotos(User user, int page, int perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        return photos.findByAuthorId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Collection> userCollections(User user, int page, int perPage) {
        return collections.findUserCollections(user.getId(), PageRequest.of(page - 1, perPage));
    }

    @Transactional(readOnly = true)
    public Page<Follow> userFollowers(User user, int page, int perPage) {
        return follows.findFollowers(user.getId(), PageRequest.of(page - 1, perPage));
    }

    @Transactional(readOnly = true)
    public Page<Follow> userFollowing(User user, int page, int perPage) {
        return follows.findFollowing(user.getId(), PageRequest.of(page - 1, perPage));
    }

    @Transactional
    public void delete(User user) {
        users.delete(user);
    }
}
