package moments4j;

import moments4j.model.User;
import moments4j.settings.MomentsSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Port of tests/test_user.py. */
class UserTestCase extends BaseTestCase {

    @Autowired
    private moments4j.core.Utils utils;

    @Test
    void testIndexPage() throws Exception {
        MvcResult page = client.perform(get("/user/normal")).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Normal User"));

        login("locked@helloflask.com", "123");
        page = client.perform(get("/user/locked").session(session)).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Locked User"));
        assertTrue(data.contains("Your account is locked."));
    }

    @Test
    void testShowCollections() throws Exception {
        MvcResult page = client.perform(get("/user/normal/collections")).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Normal User"));
        assertTrue(data.contains("No collection."));

        userService.collect(userOf("normal@helloflask.com"), photos.findById(1L).orElseThrow());
        page = client.perform(get("/user/normal/collections")).andReturn();
        assertFalse(page.getResponse().getContentAsString().contains("No collection."));
    }

    @Test
    void testFollow() throws Exception {
        MvcResult result = client.perform(post("/user/follow/admin").with(csrf())).andReturn();
        MvcResult followed = followRedirect(result);
        assertTrue(followed.getResponse().getContentAsString().contains("Please log in to access this page."));

        login("unconfirmed@helloflask.com", "123");
        result = client.perform(post("/user/follow/admin").session(session).with(csrf())).andReturn();
        followed = followRedirect(result);
        assertTrue(followed.getResponse().getContentAsString().contains("Please confirm your account first."));

        logout();
        login();
        result = client.perform(post("/user/follow/admin").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("User followed."));

        result = client.perform(post("/user/follow/admin").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Already followed."));

        // the follow notification was pushed to the admin
        assertEquals(1, notifications.countByReceiverIdAndReadFalse(1L));
    }

    @Test
    void testUnfollow() throws Exception {
        login();
        MvcResult result = client.perform(post("/user/unfollow/admin").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Not following yet."));

        client.perform(post("/user/follow/admin").session(session).with(csrf()));

        result = client.perform(post("/user/unfollow/admin").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("User unfollowed."));
    }

    @Test
    void testShowFollowers() throws Exception {
        MvcResult page = client.perform(get("/user/normal/followers")).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Normal User"));
        assertTrue(data.contains("No followers."));

        userService.follow(userOf("admin@helloflask.com"), userOf("normal@helloflask.com"));

        page = client.perform(get("/user/normal/followers")).andReturn();
        data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Admin"));
        assertFalse(data.contains("No followers."));
    }

    @Test
    void testShowFollowing() throws Exception {
        MvcResult page = client.perform(get("/user/normal/following")).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Normal User"));
        assertTrue(data.contains("No following."));

        userService.follow(userOf("normal@helloflask.com"), userOf("admin@helloflask.com"));

        page = client.perform(get("/user/normal/following")).andReturn();
        data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Admin"));
    }

    @Test
    void testEditProfile() throws Exception {
        login();
        MvcResult result = client.perform(post("/user/settings/profile").session(session).with(csrf())
                        .param("username", "newname")
                        .param("name", "New Name"))
                .andReturn();
        assertTrue(flashText(result).contains("Profile updated."));
        User user = userOf("normal@helloflask.com");
        assertEquals("New Name", user.getName());
        assertEquals("newname", user.getUsername());
    }

    @Test
    void testChangeAvatar() throws Exception {
        login();
        MvcResult page = client.perform(get("/user/settings/avatar").session(session)).andReturn();
        assertEquals(200, page.getResponse().getStatus());
        assertTrue(page.getResponse().getContentAsString().contains("Change Avatar"));
    }

    @Test
    void testUploadAvatar() throws Exception {
        login();
        // invalid image bytes -> flash error; valid image -> 'Image uploaded, please crop.'
        MockMultipartFile bad = new MockMultipartFile("image", "test.jpg", "image/jpeg", "abcdef".getBytes());
        MvcResult result = client.perform(multipart("/user/settings/avatar/upload").file(bad)
                        .session(session).with(csrf()))
                .andReturn();
        assertFalse(flashText(result).contains("Image uploaded, please crop."));
    }

    @Test
    void testChangePassword() throws Exception {
        User user = userOf("normal@helloflask.com");
        assertTrue(userService.validatePassword(user, "123"));

        login();
        MvcResult result = client.perform(post("/user/settings/change-password").session(session).with(csrf())
                        .param("oldPassword", "123")
                        .param("password", "new-password")
                        .param("password2", "new-password"))
                .andReturn();
        assertTrue(flashText(result).contains("Password updated."));
        user = userOf("normal@helloflask.com");
        assertTrue(userService.validatePassword(user, "new-password"));
        assertFalse(userService.validatePassword(user, "123"));
    }

    @Test
    void testChangeEmail() throws Exception {
        User user = userOf("normal@helloflask.com");
        assertEquals("normal@helloflask.com", user.getEmail());
        String token = utils.generateToken(user, MomentsSettings.Operation.CHANGE_EMAIL, Duration.ofHours(1),
                java.util.Map.of("new_email", "new@helloflask.com"));

        login();
        MvcResult result = client.perform(get("/user/change-email/" + token).session(session)).andReturn();
        assertTrue(flashText(result).contains("Email updated."));
        assertEquals("new@helloflask.com", users.findByUsername("normal").orElseThrow().getEmail());

        result = client.perform(get("/user/change-email/bad").session(session)).andReturn();
        assertTrue(flashText(result).contains("Invalid or expired token."));
    }

    @Test
    void testNotificationSetting() throws Exception {
        login();
        MvcResult result = client.perform(post("/user/settings/notification").session(session).with(csrf()))
                .andReturn();
        assertTrue(flashText(result).contains("Notification settings updated."));

        User user = userOf("normal@helloflask.com");
        assertFalse(user.isReceiveCollectNotification());
        assertFalse(user.isReceiveCommentNotification());
        assertFalse(user.isReceiveFollowNotification());

        // admin follows/collects/comments -> no notifications generated
        logout();
        login("admin@helloflask.com", "123");
        client.perform(post("/user/follow/normal").session(session).with(csrf()));
        client.perform(post("/collect/2").session(session).with(csrf()));
        client.perform(post("/photo/2/comment/new").session(session).with(csrf()).param("body", "test"));
        assertEquals(0, notifications.countByReceiverIdAndReadFalse(2L));
    }

    @Test
    void testPrivacySetting() throws Exception {
        login();
        MvcResult result = client.perform(post("/user/settings/privacy").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Privacy settings updated."));
        assertFalse(userOf("normal@helloflask.com").isPublicCollections());

        logout();
        MvcResult page = client.perform(get("/user/normal/collections")).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Normal User"));
        assertTrue(data.contains("This user's collections are private."));
    }

    @Test
    void testDeleteAccount() throws Exception {
        login();
        MvcResult result = client.perform(post("/user/settings/account/delete").session(session).with(csrf())
                        .param("username", "normal"))
                .andReturn();
        assertTrue(flashText(result).contains("Your are free, goodbye!"));
        assertFalse(users.findById(2L).isPresent());
    }
}
