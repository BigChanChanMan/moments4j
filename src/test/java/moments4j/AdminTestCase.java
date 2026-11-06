package moments4j;

import moments4j.model.Tag;
import moments4j.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Port of tests/test_admin.py. */
class AdminTestCase extends BaseTestCase {

    @BeforeEach
    void loginAdmin() throws Exception {
        login("admin@helloflask.com", "123");
    }

    @Test
    void testIndexPage() throws Exception {
        MvcResult page = client.perform(get("/admin/").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Moments Dashboard"));
    }

    @Test
    void testBadPermission() throws Exception {
        logout();
        MvcResult result = client.perform(get("/admin/").session(session)).andReturn();
        MvcResult followed = followRedirect(result);
        assertTrue(followed.getResponse().getContentAsString().contains("Please log in to access this page."));

        login();  // normal user, without MODERATE permission
        client.perform(get("/admin/").session(session))
                .andExpect(result1 -> assertEquals(403, result1.getResponse().getStatus()));
    }

    @Test
    void testEditProfileAdmin() throws Exception {
        Long lockedRoleId = roleRepo().findByName("Locked").orElseThrow().getId();
        MvcResult result = client.perform(post("/admin/profile/2").session(session).with(csrf())
                        .param("username", "newname")
                        .param("role", String.valueOf(lockedRoleId))
                        .param("confirmed", "true")
                        .param("active", "true")
                        .param("name", "New Name")
                        .param("email", "new@helloflask.com"))
                .andReturn();
        assertTrue(flashText(result).contains("Profile updated."));
        User updated = users.findByUsername("newname").orElseThrow();
        assertEquals("New Name", updated.getName());
        assertEquals("new@helloflask.com", updated.getEmail());
        assertEquals("Locked", updated.getRole().getName());
    }

    private moments4j.repo.RoleRepository roleRepo() {
        return applicationContext.getBean(moments4j.repo.RoleRepository.class);
    }

    @Test
    void testBlockUser() throws Exception {
        MvcResult result = client.perform(post("/admin/block/user/2").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Account blocked."));
        assertFalse(userOf("normal@helloflask.com").isActive());
    }

    @Test
    void testUnblockUser() throws Exception {
        User user = userOf("normal@helloflask.com");
        userService.block(user);
        assertFalse(user.isActive());

        MvcResult result = client.perform(post("/admin/unblock/user/2").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Block canceled."));
        assertTrue(userOf("normal@helloflask.com").isActive());
    }

    @Test
    void testLockUser() throws Exception {
        MvcResult result = client.perform(post("/admin/lock/user/2").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Account locked."));
        assertEquals("Locked", userOf("normal@helloflask.com").getRole().getName());
    }

    @Test
    void testUnlockUser() throws Exception {
        User user = userOf("normal@helloflask.com");
        userService.lock(user);
        assertEquals("Locked", user.getRole().getName());

        MvcResult result = client.perform(post("/admin/unlock/user/2").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Lock canceled."));
        assertEquals("User", userOf("normal@helloflask.com").getRole().getName());
    }

    @Test
    void testDeleteTag() throws Exception {
        Tag tag = new Tag();
        tag.setName("test");
        tag = tags.save(tag);
        Long tagId = tag.getId();

        MvcResult result = client.perform(post("/admin/delete/tag/" + tagId).session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Tag deleted."));
        assertFalse(tags.findById(tagId).isPresent());
    }

    @Test
    void testManageUserPage() throws Exception {
        MvcResult page = client.perform(get("/admin/manage/user").session(session)).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Users"));
        assertTrue(data.contains("Admin"));
        assertTrue(data.contains("Locked"));
        assertTrue(data.contains("Normal"));

        page = client.perform(get("/admin/manage/user").session(session).param("filter", "locked")).andReturn();
        data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Locked User"));
        assertFalse(data.contains("Normal User"));

        page = client.perform(get("/admin/manage/user").session(session).param("filter", "blocked")).andReturn();
        data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Blocked User"));
        assertFalse(data.contains("Locked User"));
        assertFalse(data.contains("Normal User"));

        page = client.perform(get("/admin/manage/user").session(session).param("filter", "administrator")).andReturn();
        data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Admin"));
        assertFalse(data.contains("Blocked User"));
        assertFalse(data.contains("Locked User"));
        assertFalse(data.contains("Normal User"));

        page = client.perform(get("/admin/manage/user").session(session).param("filter", "moderator")).andReturn();
        data = page.getResponse().getContentAsString();
        assertTrue(data.contains("Admin"));
        assertFalse(data.contains("Blocked User"));
    }

    @Test
    void testManagePhotoPage() throws Exception {
        MvcResult page = client.perform(get("/admin/manage/photo").session(session)).andReturn();
        assertEquals(302, page.getResponse().getStatus());  // default redirect to by_flag
        page = client.perform(get("/admin/manage/photo/by_flag").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Order by flag"));

        page = client.perform(get("/admin/manage/photo/by_time").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Order by time"));
    }

    @Test
    void testManageTagPage() throws Exception {
        MvcResult page = client.perform(get("/admin/manage/tag").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Tags"));
    }

    @Test
    void testManageCommentPage() throws Exception {
        MvcResult page = client.perform(get("/admin/manage/comment").session(session)).andReturn();
        assertEquals(302, page.getResponse().getStatus());
        page = client.perform(get("/admin/manage/comment/by_flag").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Order by flag"));

        page = client.perform(get("/admin/manage/comment/by_time").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Order by time"));
    }
}
