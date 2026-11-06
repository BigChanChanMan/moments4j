package moments4j;

import moments4j.model.User;
import moments4j.settings.MomentsSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Port of tests/test_auth.py. */
class AuthTestCase extends BaseTestCase {

    @Autowired
    private moments4j.core.Utils utils;

    @Test
    void testLoginNormalUser() throws Exception {
        MvcResult result = login();
        assertTrue(flashText(result).contains("Login success."));
    }

    @Test
    void testLoginLockedUser() throws Exception {
        login("locked@helloflask.com", "123");
        MvcResult page = client.perform(get("/user/locked").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Your account is locked."));
    }

    @Test
    void testLoginBlockedUser() throws Exception {
        MvcResult result = login("blocked@helloflask.com", "123");
        assertTrue(flashText(result).contains("Your account is blocked."));
    }

    @Test
    void testFailLogin() throws Exception {
        MvcResult result = login("wrong-username@helloflask.com", "wrong-password");
        assertTrue(flashText(result).contains("Invalid email or password."));
    }

    @Test
    void testLogoutUser() throws Exception {
        login();
        MvcResult result = client.perform(get("/auth/logout").session(session)).andReturn();
        // flash set via RedirectAttributes on the logout redirect
        MvcResult followed = followRedirect(result);
        String content = followed.getResponse().getContentAsString();
        assertTrue(content.contains("Logout success.") || flashText(result).contains("Logout success."));
    }

    @Test
    void testLoginProtect() throws Exception {
        MvcResult result = client.perform(get("/upload")).andReturn();
        String redirect = result.getResponse().getRedirectedUrl();
        assertTrue(redirect != null && redirect.startsWith("/auth/login"));
        // flash message lands after the redirect
        MvcResult followed = followRedirect(result);
        assertTrue(followed.getResponse().getContentAsString().contains("Please log in to access this page."));
    }

    @Test
    void testUnconfirmedUserPermission() throws Exception {
        login("unconfirmed@helloflask.com", "123");
        MvcResult result = client.perform(get("/upload").session(session)).andReturn();
        MvcResult followed = followRedirect(result);
        assertTrue(followed.getResponse().getContentAsString().contains("Please confirm your account first."));
    }

    @Test
    void testLockedUserPermission() throws Exception {
        login("locked@helloflask.com", "123");
        client.perform(get("/upload").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void testRegisterAccount() throws Exception {
        MvcResult result = client.perform(post("/auth/register").with(csrf())
                        .param("name", "Grey Li")
                        .param("email", "test@helloflask.com")
                        .param("username", "test")
                        .param("password", "12345678")
                        .param("password2", "12345678"))
                .andReturn();
        assertTrue(flashText(result).contains("Confirmation email sent, please check your inbox."));
    }

    @Test
    void testConfirmAccount() throws Exception {
        User unconfirmed = userOf("unconfirmed@helloflask.com");
        assertFalse(unconfirmed.isConfirmed());
        String token = utils.generateToken(unconfirmed, MomentsSettings.Operation.CONFIRM, Duration.ofHours(1));
        login("unconfirmed@helloflask.com", "123");
        MvcResult result = client.perform(get("/auth/confirm/" + token).session(session)).andReturn();
        assertTrue(flashText(result).contains("Account confirmed."));
        assertTrue(userOf("unconfirmed@helloflask.com").isConfirmed());
    }

    @Test
    void testBadConfirmToken() throws Exception {
        login("unconfirmed@helloflask.com", "123");
        MvcResult result = client.perform(get("/auth/confirm/bad-token").session(session)).andReturn();
        assertTrue(flashText(result).contains("Invalid or expired token."));
        assertFalse(flashText(result).contains("Account confirmed."));
    }

    @Test
    void testResetPassword() throws Exception {
        MvcResult forget = client.perform(post("/auth/forget-password").with(csrf())
                        .param("email", "normal@helloflask.com"))
                .andReturn();
        assertTrue(flashText(forget).contains("Password reset email sent, please check your inbox."));
        User user = userOf("normal@helloflask.com");
        assertTrue(userService.validatePassword(user, "123"));

        String token = utils.generateToken(user, MomentsSettings.Operation.RESET_PASSWORD, Duration.ofHours(1));
        MvcResult reset = client.perform(post("/auth/reset-password/" + token).with(csrf())
                        .param("email", "normal@helloflask.com")
                        .param("password", "new-password")
                        .param("password2", "new-password"))
                .andReturn();
        assertTrue(flashText(reset).contains("Password updated."));
        user = userOf("normal@helloflask.com");
        assertTrue(userService.validatePassword(user, "new-password"));
        assertFalse(userService.validatePassword(user, "123"));

        MvcResult bad = client.perform(post("/auth/reset-password/bad-token").with(csrf())
                        .param("email", "normal@helloflask.com")
                        .param("password", "new-password")
                        .param("password2", "new-password"))
                .andReturn();
        assertTrue(flashText(bad).contains("Invalid or expired link."));
        assertFalse(flashText(bad).contains("Password updated."));
    }
}
