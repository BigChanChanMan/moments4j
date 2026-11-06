package moments4j;

import moments4j.model.Comment;
import moments4j.model.Photo;
import moments4j.model.User;
import moments4j.repo.CommentRepository;
import moments4j.repo.NotificationRepository;
import moments4j.repo.PhotoRepository;
import moments4j.repo.TagRepository;
import moments4j.repo.UserRepository;
import moments4j.service.AppUserDetailsService;
import moments4j.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Port of tests/__init__.py BaseTestCase: seeds the same fixture data
 * (admin/normal/unconfirmed/locked/blocked users, photos, comment, tag)
 * against an in-memory database, with MockMvc playing the test client.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTestCase {

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc client;

    @Autowired
    protected UserRepository users;
    @Autowired
    protected PhotoRepository photos;
    @Autowired
    protected CommentRepository comments;
    @Autowired
    protected TagRepository tags;
    @Autowired
    protected NotificationRepository notifications;
    @Autowired
    protected UserService userService;
    @Autowired
    protected ApplicationContext applicationContext;

    /** Session of the currently logged-in user (flask test client's cookie jar). */
    protected MockHttpSession session;

    @BeforeEach
    void setUpBase() {
        client = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        seed();
    }

    @AfterEach
    void tearDownBase() {
        notifications.deleteAll();
        comments.deleteAll();
        applicationContext.getBean(moments4j.repo.FollowRepository.class).deleteAll();
        applicationContext.getBean(moments4j.repo.CollectionRepository.class).deleteAll();
        photos.deleteAll();
        tags.deleteAll();
        users.deleteAll();
        // role_permission join rows go with the roles; photo_tag rows go with the photos
        applicationContext.getBean(moments4j.repo.RoleRepository.class).deleteAll();
        applicationContext.getBean(moments4j.repo.PermissionRepository.class).deleteAll();
        // reset identity counters so each test sees ids 1..N like the sqlite tests
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(
                applicationContext.getBean(javax.sql.DataSource.class));
        for (String table : new String[] {"notification", "comment", "photo", "tag", "user", "role", "permission"}) {
            jdbc.execute("ALTER TABLE \"" + table + "\" ALTER COLUMN \"id\" RESTART WITH 1");
        }
    }

    protected void seed() {
        userService.initRole();

        User adminUser = userService.createUser("Admin", "admin@helloflask.com", "admin", "123", true);
        User normalUser = userService.createUser("Normal User", "normal@helloflask.com", "normal", "123", true);
        userService.createUser("Unconfirmed", "unconfirmed@helloflask.com", "unconfirmed", "123", false);
        User lockedUser = userService.createUser("Locked User", "locked@helloflask.com", "locked", "123", true);
        userService.lock(lockedUser);
        users.save(lockedUser);
        User blockedUser = userService.createUser("Blocked User", "blocked@helloflask.com", "blocked", "123", true);
        userService.block(blockedUser);
        users.save(blockedUser);

        Photo photo = new Photo();
        photo.setFilename("test.jpg");
        photo.setFilenameS("test_s.jpg");
        photo.setFilenameM("test_m.jpg");
        photo.setDescription("Photo 1");
        photo.setAuthor(adminUser);
        photo = photos.save(photo);

        Photo photo2 = new Photo();
        photo2.setFilename("test2.jpg");
        photo2.setFilenameS("test_s2.jpg");
        photo2.setFilenameM("test_m2.jpg");
        photo2.setDescription("Photo 2");
        photo2.setAuthor(normalUser);
        photos.save(photo2);

        Comment comment = new Comment();
        comment.setBody("test comment body");
        comment.setPhoto(photo);
        comment.setAuthor(normalUser);
        comments.save(comment);

        moments4j.model.Tag tag = new moments4j.model.Tag();
        tag.setName("test tag");
        tag = tags.save(tag);
        photo.getTags().add(tag);
        photos.save(photo);
    }

    // ---- client helpers (tests/__init__.py login/logout) ----

    /** POST /auth/login and keep the session (flask client's follow_redirects login). */
    protected MvcResult login() throws Exception {
        return login("normal@helloflask.com", "123");
    }

    protected MvcResult login(String email, String password) throws Exception {
        session = new MockHttpSession();
        return client.perform(post("/auth/login").session(session).with(csrf())
                        .param("email", email).param("password", password))
                .andReturn();
    }

    protected void logout() throws Exception {
        client.perform(get("/auth/logout").session(session).with(csrf()));
        session = new MockHttpSession();
    }

    /** The seeded user entity for an email. */
    protected User userOf(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    /** A request post-processor signing in a seeded user (no login POST needed). */
    protected org.springframework.test.web.servlet.request.RequestPostProcessor asUser(User user) {
        return user(new AppUserDetailsService.AuthUser(user));
    }

    /** Flash messages of a redirect result, as [(category, message), ...]. */
    @SuppressWarnings("unchecked")
    protected java.util.List<Object[]> flashesOf(MvcResult result) {
        org.springframework.web.servlet.FlashMap flashMap = (org.springframework.web.servlet.FlashMap)
                result.getRequest().getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
        if (flashMap == null) {
            return java.util.List.of();
        }
        java.util.List<Object[]> flashes = (java.util.List<Object[]>) flashMap.get("flashes");
        return flashes == null ? java.util.List.of() : flashes;
    }

    protected String flashText(MvcResult result) {
        StringBuilder sb = new StringBuilder();
        for (Object[] f : flashesOf(result)) {
            sb.append(f[1]).append('\n');
        }
        return sb.toString();
    }

    /** Follows one redirect of a result with the current session (follow_redirects=True). */
    protected MvcResult followRedirect(MvcResult result) throws Exception {
        String url = result.getResponse().getRedirectedUrl();
        if (url == null) {
            return result;
        }
        if (url.startsWith("http")) {
            url = java.net.URI.create(url).getPath();
        }
        var builder = get(url);
        if (session != null) {
            builder = builder.session(session);
        } else if (result.getRequest().getSession(false) != null) {
            builder = builder.session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession());
        }
        return client.perform(builder).andReturn();
    }
}
