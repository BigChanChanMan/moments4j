package moments4j;

import moments4j.model.Comment;
import moments4j.model.Photo;
import moments4j.model.User;
import moments4j.repo.CommentRepository;
import moments4j.repo.NotificationRepository;
import moments4j.repo.PhotoRepository;
import moments4j.repo.TagRepository;
import moments4j.repo.UserRepository;
import moments4j.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Port of tests/test_ui.py: drives a real headless Chrome against the running app
 * (Spring Boot starts itself on a random port here, replacing the original's
 * "run the app first with flask run" instruction). E2E tests:
 *   RUN_E2E=true mvn test -Dtest=UiTestCase
 * Selenium Manager downloads the matching chromedriver on first run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "RUN_E2E", matches = "(?i)true|1|yes",
        disabledReason = "E2E UI test; set RUN_E2E=true to run (needs local Chrome)")
class UiTestCase {

    @LocalServerPort
    int port;

    String base;
    WebDriver browser;

    @Autowired
    org.springframework.context.ApplicationContext applicationContext;
    @Autowired
    UserService userService;
    @Autowired
    UserRepository users;
    @Autowired
    PhotoRepository photos;
    @Autowired
    CommentRepository comments;
    @Autowired
    TagRepository tags;
    @Autowired
    NotificationRepository notifications;

    @BeforeEach
    void setUp() {
        // ---- seed (tests/app.py): admin/123, normal user, photo 1 (admin), photo 2 (normal), comment ----
        userService.initRole();
        User adminUser = userService.createUser("Admin", "admin@helloflask.com", "admin", "123", true);
        User normalUser = userService.createUser("Normal User", "normal@helloflask.com", "normal", "123", true);
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

        // ---- browser (setUp of test_ui.py) ----
        base = "http://localhost:" + port;
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        // maximize_window() is unreliable in new headless chrome — set the size explicitly
        options.addArguments("--window-size=1920,1080");
        try {
            browser = new ChromeDriver(options);
        } catch (IllegalStateException | org.openqa.selenium.WebDriverException e) {
            assumeTrue(false, "Web browser not available: " + e.getMessage());
            return;
        }
        // (window size set via --window-size; maximize is a no-op/glitchy in new headless)
        try {
            browser.get(base + "/");
        } catch (org.openqa.selenium.WebDriverException e) {
            browser.quit();
            assumeTrue(false, "Server not running: " + e.getMessage());
            return;
        }
        login();
    }

    @AfterEach
    void tearDown() {
        if (browser != null) {
            browser.quit();
        }
        // reset ids to 1..N like flask's per-test in-memory sqlite (tests rely on photo id 1)
        notifications.deleteAll();
        comments.deleteAll();
        applicationContext.getBean(moments4j.repo.FollowRepository.class).deleteAll();
        applicationContext.getBean(moments4j.repo.CollectionRepository.class).deleteAll();
        photos.deleteAll();
        tags.deleteAll();
        users.deleteAll();
        applicationContext.getBean(moments4j.repo.RoleRepository.class).deleteAll();
        applicationContext.getBean(moments4j.repo.PermissionRepository.class).deleteAll();
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(
                applicationContext.getBean(javax.sql.DataSource.class));
        for (String table : new String[] {"notification", "comment", "photo", "tag", "user", "role", "permission"}) {
            jdbc.execute("ALTER TABLE \"" + table + "\" ALTER COLUMN \"id\" RESTART WITH 1");
        }
    }

    private void login() {
        browser.get(base + "/auth/login");
        sleep();
        browser.findElement(By.name("email")).sendKeys("admin@helloflask.com");
        browser.findElement(By.name("password")).sendKeys("123");
        browser.findElement(By.name("submit")).click();
        sleep();
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void testLogin() {
        System.out.println("XX title=" + browser.getTitle() + " url=" + browser.getCurrentUrl());
        assertTrue(browser.getTitle().contains("Home"));
        assertTrue(browser.getPageSource().contains("Login success"));
    }

    @Test
    void testHomePageCollectUncollect() {
        browser.get(base + "/");
        sleep();
        WebElement collectBtn = browser.findElement(By.className("collect-btn"));
        WebElement uncollectBtn = browser.findElement(By.className("uncollect-btn"));
        long originCollectCount = Long.parseLong(
                browser.findElement(By.id("collectors-count-1")).getText());
        // collect the photo
        collectBtn.click();
        sleep();
        assertTrue(browser.getPageSource().contains("Photo collected"));
        long newCollectCount = Long.parseLong(
                browser.findElement(By.id("collectors-count-1")).getText());
        assertEquals(originCollectCount + 1, newCollectCount);
        assertEquals("block", uncollectBtn.getCssValue("display"));
        assertEquals("none", collectBtn.getCssValue("display"));
        // uncollect the photo
        uncollectBtn.click();
        sleep();
        assertTrue(browser.getPageSource().contains("Collect canceled"));
        newCollectCount = Long.parseLong(
                browser.findElement(By.id("collectors-count-1")).getText());
        assertEquals(originCollectCount, newCollectCount);
        assertEquals("block", collectBtn.getCssValue("display"));
        assertEquals("none", uncollectBtn.getCssValue("display"));
    }

    @Test
    void testPhotoPageFollowUnfollow() {
        browser.get(base + "/photo/1");
        sleep();
        // hover the mouse over the user avatar
        WebElement userAvatar = browser.findElement(By.className("avatar-s"));
        new Actions(browser).moveToElement(userAvatar).perform();
        sleep();
        assertTrue(browser.getPageSource().contains("Homepage"));
        WebElement popover = browser.findElement(By.className("popover"));
        assertEquals("block", popover.getCssValue("display"));
        // test follow
        long originFollowerCount = Long.parseLong(
                browser.findElement(By.id("followers-count-2")).getText());
        popover.findElement(By.xpath("//a[@title='Follow']")).click();
        sleep();
        assertEquals("none", popover.findElement(By.className("follow-btn")).getCssValue("display"));
        assertEquals("inline-block",
                popover.findElement(By.className("unfollow-btn")).getCssValue("display"));
        assertTrue(browser.getPageSource().contains("User followed"));
        long newFollowerCount = Long.parseLong(
                browser.findElement(By.id("followers-count-2")).getText());
        assertEquals(originFollowerCount + 1, newFollowerCount);
        // test unfollow
        originFollowerCount = newFollowerCount;
        popover.findElement(By.xpath("//a[@title='Unfollow']")).click();
        sleep();
        assertTrue(browser.getPageSource().contains("Follow canceled"));
        assertEquals("none", popover.findElement(By.className("unfollow-btn")).getCssValue("display"));
        assertEquals("inline-block",
                popover.findElement(By.className("follow-btn")).getCssValue("display"));
        newFollowerCount = Long.parseLong(
                browser.findElement(By.id("followers-count-2")).getText());
        assertEquals(originFollowerCount - 1, newFollowerCount);
    }

    @Test
    void testPhotoPageEditDescription() {
        browser.get(base + "/photo/1");
        sleep();

        WebElement editDescriptionBtn = browser.findElement(By.id("description-btn"));
        WebElement descriptionForm = browser.findElement(By.id("description-form"));
        WebElement description = browser.findElement(By.id("description"));
        assertEquals("inline", editDescriptionBtn.getCssValue("display"));
        assertEquals("block", description.getCssValue("display"));
        assertEquals("none", descriptionForm.getCssValue("display"));

        editDescriptionBtn.click();
        sleep();
        assertEquals("none", description.getCssValue("display"));
        assertEquals("block", descriptionForm.getCssValue("display"));
        descriptionForm.findElement(By.name("description")).sendKeys(" test description");
        descriptionForm.findElement(By.name("submit")).click();
        sleep();
        assertEquals("block", browser.findElement(By.id("description")).getCssValue("display"));
        assertEquals("none", browser.findElement(By.id("description-form")).getCssValue("display"));
        assertTrue(browser.getPageSource().contains("test description"));
    }

    @Test
    void testPhotoPageEditTag() {
        browser.get(base + "/photo/1");
        sleep();

        WebElement editTagBtn = browser.findElement(By.id("tag-btn"));
        WebElement tagForm = browser.findElement(By.id("tag-form"));
        WebElement tagsElem = browser.findElement(By.id("tags"));
        assertEquals("inline", editTagBtn.getCssValue("display"));
        assertEquals("block", tagsElem.getCssValue("display"));
        assertEquals("none", tagForm.getCssValue("display"));

        editTagBtn.click();
        sleep();
        assertEquals("none", tagsElem.getCssValue("display"));
        assertEquals("block", tagForm.getCssValue("display"));
        tagForm.findElement(By.name("tag")).sendKeys("selenium");
        tagForm.findElement(By.name("submit")).click();
        sleep();

        assertEquals("block", browser.findElement(By.id("tags")).getCssValue("display"));
        assertEquals("none", browser.findElement(By.id("tag-form")).getCssValue("display"));
        assertTrue(browser.getPageSource().contains("selenium"));

        browser.findElement(By.id("tag-btn")).click();
        browser.findElement(By.id("tag-form"))
                .findElement(By.xpath("//a[@title='Delete tag']")).click();
        sleep();
        browser.findElement(By.id("delete-modal"))
                .findElement(By.className("btn-confirm")).click();
        sleep();
        assertFalse(browser.getPageSource().contains("selenium"));
    }

    @Test
    void testPhotoPageSharePhoto() {
        browser.get(base + "/photo/1");
        sleep();

        WebElement shareModal = browser.findElement(By.id("share-modal"));
        assertEquals("none", shareModal.getCssValue("display"));

        // find the share button based on the title attribute
        browser.findElement(By.xpath("//a[@title='Share']")).click();
        sleep();
        assertEquals("block", shareModal.getCssValue("display"));
    }

    @Test
    void testPhotoPageDeletePhoto() {
        browser.get(base + "/photo/1");

        WebElement deleteModal = browser.findElement(By.id("delete-modal"));
        assertEquals("none", deleteModal.getCssValue("display"));

        // find the delete button based on the title attribute
        browser.findElement(By.xpath("//a[@title='Delete']")).click();
        sleep();
        assertEquals("block", deleteModal.getCssValue("display"));
    }
}
