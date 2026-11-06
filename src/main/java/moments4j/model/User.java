package moments4j.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Port of models.py User. Behavior methods (follow/collect/lock/...) live in
 * moments4j.service.UserService since they need repositories.
 */
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 255)
    private String website;

    @Column(length = 120)
    private String bio;

    @Column(length = 50)
    private String location;

    @Column(name = "member_since", nullable = false)
    private LocalDateTime memberSince = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "avatar_s", length = 64)
    private String avatarS;

    @Column(name = "avatar_m", length = 64)
    private String avatarM;

    @Column(name = "avatar_l", length = 64)
    private String avatarL;

    @Column(name = "avatar_raw", length = 64)
    private String avatarRaw;

    @Column(nullable = false)
    private boolean confirmed = false;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "public_collections", nullable = false)
    private boolean publicCollections = true;

    @Column(name = "receive_comment_notification", nullable = false)
    private boolean receiveCommentNotification = true;

    @Column(name = "receive_follow_notification", nullable = false)
    private boolean receiveFollowNotification = true;

    @Column(name = "receive_collect_notification", nullable = false)
    private boolean receiveCollectNotification = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Photo> photos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Notification> notifications = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Collection> collections = new LinkedHashSet<>();

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Follow> following = new LinkedHashSet<>();

    @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Follow> followers = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(LocalDateTime memberSince) {
        this.memberSince = memberSince;
    }

    public String getAvatarS() {
        return avatarS;
    }

    public void setAvatarS(String avatarS) {
        this.avatarS = avatarS;
    }

    public String getAvatarM() {
        return avatarM;
    }

    public void setAvatarM(String avatarM) {
        this.avatarM = avatarM;
    }

    public String getAvatarL() {
        return avatarL;
    }

    public void setAvatarL(String avatarL) {
        this.avatarL = avatarL;
    }

    public String getAvatarRaw() {
        return avatarRaw;
    }

    public void setAvatarRaw(String avatarRaw) {
        this.avatarRaw = avatarRaw;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPublicCollections() {
        return publicCollections;
    }

    public void setPublicCollections(boolean publicCollections) {
        this.publicCollections = publicCollections;
    }

    public boolean isReceiveCommentNotification() {
        return receiveCommentNotification;
    }

    public void setReceiveCommentNotification(boolean receiveCommentNotification) {
        this.receiveCommentNotification = receiveCommentNotification;
    }

    public boolean isReceiveFollowNotification() {
        return receiveFollowNotification;
    }

    public void setReceiveFollowNotification(boolean receiveFollowNotification) {
        this.receiveFollowNotification = receiveFollowNotification;
    }

    public boolean isReceiveCollectNotification() {
        return receiveCollectNotification;
    }

    public void setReceiveCollectNotification(boolean receiveCollectNotification) {
        this.receiveCollectNotification = receiveCollectNotification;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return role != null && "Administrator".equals(role.getName());
    }

    @Override
    public String toString() {
        return "User " + id + ": " + username;
    }
}
