package moments4j.blueprints;

import jakarta.servlet.http.HttpServletRequest;
import moments4j.core.ViewHelper;
import moments4j.core.security.CurrentUserService;
import moments4j.model.Photo;
import moments4j.model.User;
import moments4j.repo.PhotoRepository;
import moments4j.repo.UserRepository;
import moments4j.service.NotificationService;
import moments4j.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Port of moments/blueprints/ajax.py. JSON errors are returned as
 * {'message': ...} with status codes, matching the Flask version.
 */
@Controller
@RequestMapping("/ajax")
public class AjaxController {

    private final PhotoRepository photos;
    private final UserRepository users;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public AjaxController(PhotoRepository photos, UserRepository users, UserService userService,
                          NotificationService notificationService, CurrentUserService currentUserService) {
        this.photos = photos;
        this.users = users;
        this.userService = userService;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/notifications-count")
    public ResponseEntity<?> notificationsCount() {
        User current = currentUserService.get();
        if (current == null) {
            return json("Login required.", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(Map.of("count", userService.notificationsCount(current)));
    }

    @GetMapping("/profile/{userId}")
    public String getProfile(@PathVariable Long userId, org.springframework.ui.Model model) {
        User user = users.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("user", user);
        return "main/profile_popup";
    }

    @GetMapping("/followers-count/{userId}")
    public ResponseEntity<?> followersCount(@PathVariable Long userId) {
        User user = users.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(Map.of("count", userService.followersCount(user)));
    }

    @GetMapping("/collectors-count/{photoId}")
    public ResponseEntity<?> collectorsCount(@PathVariable Long photoId) {
        Photo photo = photos.findById(photoId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(Map.of("count", userService.photoCollectorsCount(photo)));
    }

    @PostMapping("/collect/{photoId}")
    public ResponseEntity<?> collect(@PathVariable Long photoId) {
        User current = currentUserService.get();
        if (current == null) {
            return json("Login required.", HttpStatus.FORBIDDEN);
        }
        if (!current.isConfirmed()) {
            return json("Confirm account required.", HttpStatus.BAD_REQUEST);
        }
        if (!userService.can(current, "COLLECT")) {
            return json("No permission.", HttpStatus.FORBIDDEN);
        }
        Photo photo = photos.findById(photoId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (userService.isCollecting(current, photo)) {
            return json("Already collected.", HttpStatus.BAD_REQUEST);
        }
        userService.collect(current, photo);
        if (!current.getId().equals(photo.getAuthor().getId())) {
            notificationService.pushCollectNotification(current, photoId, photo.getAuthor(),
                    "/user/" + current.getUsername(), "/photo/" + photoId);
        }
        return ResponseEntity.ok(Map.of("message", "Photo collected."));
    }

    @PostMapping("/uncollect/{photoId}")
    public ResponseEntity<?> uncollect(@PathVariable Long photoId) {
        User current = currentUserService.get();
        if (current == null) {
            return json("Login required.", HttpStatus.FORBIDDEN);
        }
        Photo photo = photos.findById(photoId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!userService.isCollecting(current, photo)) {
            return json("Not collect yet.", HttpStatus.BAD_REQUEST);
        }
        userService.uncollect(current, photo);
        return ResponseEntity.ok(Map.of("message", "Collect canceled."));
    }

    @PostMapping("/follow/{username}")
    public ResponseEntity<?> follow(@PathVariable String username) {
        User current = currentUserService.get();
        if (current == null) {
            return json("Login required.", HttpStatus.FORBIDDEN);
        }
        if (!current.isConfirmed()) {
            return json("Confirm account required.", HttpStatus.BAD_REQUEST);
        }
        if (!userService.can(current, "FOLLOW")) {
            return json("No permission.", HttpStatus.FORBIDDEN);
        }
        User user = users.findByUsername(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (userService.isFollowing(current, user)) {
            return json("Already followed.", HttpStatus.BAD_REQUEST);
        }
        userService.follow(current, user);
        notificationService.pushFollowNotification(current, user, "/user/" + current.getUsername());
        return ResponseEntity.ok(Map.of("message", "User followed."));
    }

    @PostMapping("/unfollow/{username}")
    public ResponseEntity<?> unfollow(@PathVariable String username) {
        User current = currentUserService.get();
        if (current == null) {
            return json("Login required.", HttpStatus.FORBIDDEN);
        }
        User user = users.findByUsername(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!userService.isFollowing(current, user)) {
            return json("Not following yet.", HttpStatus.BAD_REQUEST);
        }
        userService.unfollow(current, user);
        return ResponseEntity.ok(Map.of("message", "Follow canceled."));
    }

    private ResponseEntity<Map<String, String>> json(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
