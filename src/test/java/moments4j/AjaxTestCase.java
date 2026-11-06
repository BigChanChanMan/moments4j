package moments4j;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Port of tests/test_ajax.py. */
class AjaxTestCase extends BaseTestCase {

    @Test
    void testNotificationsCount() throws Exception {
        MvcResult result = client.perform(get("/ajax/notifications-count")).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Login required."));

        login();
        result = client.perform(get("/ajax/notifications-count").session(session)).andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    void testGetProfile() throws Exception {
        MvcResult result = client.perform(get("/ajax/profile/1")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Admin"));
    }

    @Test
    void testFollowersCount() throws Exception {
        MvcResult result = client.perform(get("/ajax/followers-count/1")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("\"count\":0"));

        userService.follow(userOf("normal@helloflask.com"), userOf("admin@helloflask.com"));

        result = client.perform(get("/ajax/followers-count/1")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("\"count\":1"));
    }

    @Test
    void testCollectorsCount() throws Exception {
        MvcResult result = client.perform(get("/ajax/collectors-count/1")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("\"count\":0"));

        userService.collect(userOf("admin@helloflask.com"), photos.findById(1L).orElseThrow());

        result = client.perform(get("/ajax/collectors-count/1")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("\"count\":1"));
    }

    @Test
    void testCollect() throws Exception {
        MvcResult result = client.perform(post("/ajax/collect/1").with(csrf())).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Login required."));

        login("unconfirmed@helloflask.com", "123");
        result = client.perform(post("/ajax/collect/1").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Confirm account required."));
        logout();

        login();
        result = client.perform(post("/ajax/collect/1").session(session).with(csrf())).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Photo collected."));

        result = client.perform(post("/ajax/collect/1").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Already collected."));
    }

    @Test
    void testUncollect() throws Exception {
        MvcResult result = client.perform(post("/ajax/uncollect/1").with(csrf())).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Login required."));

        login();
        result = client.perform(post("/ajax/uncollect/1").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Not collect yet."));

        userService.collect(userOf("normal@helloflask.com"), photos.findById(1L).orElseThrow());

        result = client.perform(post("/ajax/uncollect/1").session(session).with(csrf())).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Collect canceled."));
    }

    @Test
    void testFollow() throws Exception {
        MvcResult result = client.perform(post("/ajax/follow/admin").with(csrf())).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Login required."));

        login("unconfirmed@helloflask.com", "123");
        result = client.perform(post("/ajax/follow/admin").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Confirm account required."));
        logout();

        login();
        result = client.perform(post("/ajax/follow/normal").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Already followed."));

        result = client.perform(post("/ajax/follow/admin").session(session).with(csrf())).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("User followed."));
    }

    @Test
    void testUnfollow() throws Exception {
        MvcResult result = client.perform(post("/ajax/unfollow/admin").with(csrf())).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Login required."));

        login();
        result = client.perform(post("/ajax/unfollow/admin").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Not following yet."));

        userService.follow(userOf("normal@helloflask.com"), userOf("admin@helloflask.com"));

        result = client.perform(post("/ajax/unfollow/admin").session(session).with(csrf())).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Follow canceled."));
    }
}
