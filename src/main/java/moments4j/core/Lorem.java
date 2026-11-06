package moments4j.core;

import moments4j.model.*;
import moments4j.repo.*;
import moments4j.service.UserService;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneOffset;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Port of moments/lorem.py.
 */
@Component
public class Lorem {

    private final Faker fake = new Faker();
    private final Random random = new Random();

    private final UserService userService;
    private final UserRepository users;
    private final TagRepository tags;
    private final PhotoRepository photos;
    private final CommentRepository comments;
    private final NotificationRepository notifications;
    private final MomentsSettingsHolder settingsHolder;

    // ponytail: settings access without a constructor cycle — simple holder
    public Lorem(UserService userService, UserRepository users, TagRepository tags, PhotoRepository photos,
                 CommentRepository comments, NotificationRepository notifications,
                 MomentsSettingsHolder settingsHolder) {
        this.userService = userService;
        this.users = users;
        this.tags = tags;
        this.photos = photos;
        this.comments = comments;
        this.notifications = notifications;
        this.settingsHolder = settingsHolder;
    }

    @Transactional
    public void fakeAdmin() {
        User admin = userService.createUser("Grey Li", "admin@helloflask.com", "greyli",
                "moments", true);
        admin.setBio(fake.lorem().sentence());
        admin.setWebsite("https://greyli.com");

        Notification notification = new Notification();
        notification.setMessage("Hello, welcome to Moments.");
        notification.setReceiver(admin);
        notifications.save(notification);
        users.save(admin);
    }

    @Transactional
    public void fakeUser(int count) {
        for (int i = 0; i < count; i++) {
            String email = fake.internet().emailAddress();
            String username = fake.name().username().replaceAll("[^a-zA-Z0-9]", "");
            if (username.isBlank()) {
                username = "user" + i + random.nextInt(1000);
            }
            username = username.substring(0, Math.min(20, username.length()));
            if (users.findByUsername(username).isPresent() || users.findByEmailIgnoreCase(email).isPresent()) {
                continue;  // IntegrityError skip
            }
            User user = userService.createUser(fake.name().fullName(), email, username, "123456", true);
            user.setBio(fake.lorem().sentence());
            user.setLocation(fake.address().city());
            user.setWebsite(fake.internet().url());
            user.setMemberSince(java.time.LocalDateTime.now(ZoneOffset.UTC)
                    .minusDays(random.nextInt(3650)).withNano(0));
            users.save(user);
        }
    }

    @Transactional
    public void fakeFollow(int count) {
        for (int i = 0; i < count; i++) {
            User user = randomUser();
            User user2 = randomUser();
            if (user != null && user2 != null) {
                userService.follow(user, user2);
            }
        }
    }

    @Transactional
    public void fakeTag(int count) {
        for (int i = 0; i < count; i++) {
            String name = fake.lorem().word();
            if (tags.findByName(name).isPresent()) {
                continue;  // IntegrityError skip
            }
            Tag tag = new Tag();
            tag.setName(name);
            tags.save(tag);
        }
    }

    @Transactional
    public void fakePhoto(int count) {
        Path uploadPath = Path.of(settingsHolder.get().getUploadPath());
        try {
            java.nio.file.Files.createDirectories(uploadPath);
        } catch (IOException ignored) {
        }
        for (int i = 0; i < count; i++) {
            String filename = "random_" + i + ".jpg";
            BufferedImage img = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(128 + random.nextInt(128), 128 + random.nextInt(128),
                    128 + random.nextInt(128)));
            g.fillRect(0, 0, 800, 800);
            g.dispose();
            try {
                ImageIO.write(img, "jpg", uploadPath.resolve(filename).toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            User user = randomUser();
            Photo photo = new Photo();
            photo.setDescription(fake.lorem().paragraph(2));
            photo.setFilename(filename);
            photo.setFilenameM(filename);
            photo.setFilenameS(filename);
            photo.setAuthor(user);
            photo.setCreatedAt(java.time.LocalDateTime.now(ZoneOffset.UTC)
                    .minusDays(random.nextInt(365)).withNano(0));

            for (int j = 0, n = 1 + random.nextInt(5); j < n; j++) {
                Tag tag = randomTag();
                if (tag != null && !photo.getTags().contains(tag)) {
                    photo.getTags().add(tag);
                }
            }
            photos.saveAndFlush(photo);
        }
    }

    @Transactional
    public void fakeCollect(int count) {
        for (int i = 0; i < count; i++) {
            User user = randomUser();
            Photo photo = randomPhoto();
            if (user != null && photo != null) {
                userService.collect(user, photo);
            }
        }
    }

    @Transactional
    public void fakeComment(int count) {
        for (int i = 0; i < count; i++) {
            User user = randomUser();
            Photo photo = randomPhoto();
            if (user == null || photo == null) {
                continue;
            }
            Comment comment = new Comment();
            comment.setAuthor(user);
            comment.setBody(fake.lorem().sentence());
            comment.setPhoto(photo);
            comment.setCreatedAt(java.time.LocalDateTime.now(ZoneOffset.UTC)
                    .minusDays(random.nextInt(365)).withNano(0));
            comments.save(comment);
        }
    }

    private User randomUser() {
        List<Long> ids = users.findAll().stream().map(User::getId).toList();
        if (ids.isEmpty()) {
            return null;
        }
        return users.findById(ids.get(random.nextInt(ids.size()))).orElse(null);
    }

    private Photo randomPhoto() {
        long count = photos.count();
        if (count == 0) {
            return null;
        }
        return photos.findById(ThreadLocalRandom.current().nextLong(1, count + 1)).orElse(null);
    }

    private Tag randomTag() {
        long count = tags.count();
        if (count == 0) {
            return null;
        }
        return tags.findById(ThreadLocalRandom.current().nextLong(1, count + 1)).orElse(null);
    }

    /** Minimal holder to read settings inside @Component without cycles. */
    @Component
    public static class MomentsSettingsHolder {
        private final moments4j.settings.MomentsSettings settings;

        public MomentsSettingsHolder(moments4j.settings.MomentsSettings settings) {
            this.settings = settings;
        }

        public moments4j.settings.MomentsSettings get() {
            return settings;
        }
    }
}
