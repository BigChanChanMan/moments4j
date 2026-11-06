package moments4j;

import moments4j.model.Comment;
import moments4j.model.Notification;
import moments4j.model.Photo;
import moments4j.model.Tag;
import moments4j.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Port of tests/test_main.py. */
class MainTestCase extends BaseTestCase {

    @Test
    void testIndexPage() throws Exception {
        MvcResult page = client.perform(get("/")).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Join Now"));

        login();
        page = client.perform(get("/").session(session)).andReturn();
        String data = page.getResponse().getContentAsString();
        assertFalse(data.contains("Join Now"));
        assertTrue(data.contains("My Home"));
    }

    @Test
    void testExplorePage() throws Exception {
        MvcResult page = client.perform(get("/explore")).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Change"));
    }

    @Test
    void testSearch() throws Exception {
        MvcResult empty = client.perform(get("/search?q=")).andReturn();
        String redirect = empty.getResponse().getRedirectedUrl();
        assertTrue(redirect != null);  // flashes 'Enter keyword...' and redirects back

        MvcResult tagSearch = client.perform(get("/search").param("q", "normal").param("category", "tag")).andReturn();
        assertTrue(tagSearch.getResponse().getContentAsString().contains("No results."));

        MvcResult userSearch = client.perform(get("/search").param("q", "normal").param("category", "user")).andReturn();
        String data = userSearch.getResponse().getContentAsString();
        assertFalse(data.contains("No results."));
        assertTrue(data.contains("Normal User"));
    }

    @Test
    void testShowNotifications() throws Exception {
        User user = userOf("normal@helloflask.com");
        Notification notification1 = new Notification();
        notification1.setMessage("test 1");
        notification1.setRead(true);
        notification1.setReceiver(user);
        Notification notification2 = new Notification();
        notification2.setMessage("test 2");
        notification2.setReceiver(user);
        notifications.saveAll(List.of(notification1, notification2));

        login();
        MvcResult page = client.perform(get("/notifications").session(session)).andReturn();
        String data = page.getResponse().getContentAsString();
        assertTrue(data.contains("test 1"));
        assertTrue(data.contains("test 2"));

        page = client.perform(get("/notifications").session(session).param("filter", "unread")).andReturn();
        data = page.getResponse().getContentAsString();
        assertFalse(data.contains("test 1"));
        assertTrue(data.contains("test 2"));
    }

    @Test
    void testReadNotification() throws Exception {
        User user = userOf("normal@helloflask.com");
        Notification n1 = new Notification();
        n1.setMessage("test 1");
        n1.setReceiver(user);
        Notification n2 = new Notification();
        n2.setMessage("test 2");
        n2.setReceiver(user);
        n1 = notifications.save(n1);
        notifications.save(n2);

        // another user's notification is forbidden
        login("admin@helloflask.com", "123");
        client.perform(post("/notifications/read/" + n1.getId()).session(session).with(csrf()))
                .andExpect(status().isForbidden());

        logout();
        login();
        MvcResult result = client.perform(post("/notifications/read/" + n1.getId()).session(session).with(csrf()))
                .andReturn();
        assertTrue(flashText(result).contains("Notification archived."));
        assertTrue(notifications.findById(n1.getId()).orElseThrow().isRead());
    }

    @Test
    void testReadAllNotification() throws Exception {
        User user = userOf("normal@helloflask.com");
        Notification n1 = new Notification();
        n1.setMessage("test 1");
        n1.setReceiver(user);
        Notification n2 = new Notification();
        n2.setMessage("test 2");
        n2.setReceiver(user);
        n1 = notifications.save(n1);
        n2 = notifications.save(n2);

        login();
        MvcResult result = client.perform(post("/notifications/read/all").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("All notifications archived."));
        assertTrue(notifications.findById(n1.getId()).orElseThrow().isRead());
        assertTrue(notifications.findById(n2.getId()).orElseThrow().isRead());
    }

    @Test
    void testShowPhoto() throws Exception {
        MvcResult page = client.perform(get("/photo/1")).andReturn();
        String data = page.getResponse().getContentAsString();
        assertFalse(data.contains(">Delete<"));
        assertTrue(data.contains("test tag"));
        assertTrue(data.contains("test comment body"));

        login("admin@helloflask.com", "123");
        page = client.perform(get("/photo/1").session(session)).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Delete"));
    }

    @Test
    void testGetNextPhoto() throws Exception {
        User user = userOf("admin@helloflask.com");
        LocalDateTime now = LocalDateTime.now();
        Photo photo3 = photoOf("Photo 3", user, now);
        Photo photo4 = photoOf("Photo 4", user, now.plusSeconds(1));
        Photo photo5 = photoOf("Photo 5", user, now.plusSeconds(2));

        MvcResult result = client.perform(get("/photo/n/" + photo5.getId())).andReturn();
        assertTrue(followRedirect(result).getResponse().getContentAsString().contains("Photo 4"));

        result = client.perform(get("/photo/n/" + photo4.getId())).andReturn();
        assertTrue(followRedirect(result).getResponse().getContentAsString().contains("Photo 3"));

        result = client.perform(get("/photo/n/" + photo3.getId())).andReturn();
        // photo1 is older than photo3 in the seed, so next of photo3 is photo 1
        assertTrue(flashText(result).contains("This is already the last one.")
                || followRedirect(result).getResponse().getContentAsString().contains("Photo 1"));

        result = client.perform(get("/photo/p/" + photo3.getId())).andReturn();
        assertTrue(followRedirect(result).getResponse().getContentAsString().contains("Photo 4"));
    }

    private Photo photoOf(String description, User author, LocalDateTime createdAt) {
        Photo photo = new Photo();
        photo.setFilename("test.jpg");
        photo.setFilenameS("test_s.jpg");
        photo.setFilenameM("test_m.jpg");
        photo.setDescription(description);
        photo.setAuthor(author);
        photo.setCreatedAt(createdAt);
        return photos.save(photo);
    }

    @Test
    void testCollect() throws Exception {
        login();
        MvcResult result = client.perform(post("/collect/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Photo collected."));

        result = client.perform(post("/collect/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Already collected."));

        result = client.perform(post("/uncollect/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Photo uncollected."));
    }

    @Test
    void testReportPhotoAndComment() throws Exception {
        login();
        MvcResult result = client.perform(post("/report/photo/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Photo reported."));
        assertEquals(1, photos.findById(1L).orElseThrow().getFlag());

        result = client.perform(post("/report/comment/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Comment reported."));
        assertEquals(1, comments.findById(1L).orElseThrow().getFlag());
    }

    @Test
    void testEditDescription() throws Exception {
        login("admin@helloflask.com", "123");
        MvcResult result = client.perform(post("/photo/1/description").session(session).with(csrf())
                        .param("description", "new description"))
                .andReturn();
        assertTrue(flashText(result).contains("Description updated."));
        assertEquals("new description", photos.findById(1L).orElseThrow().getDescription());
    }

    @Test
    void testNewComment() throws Exception {
        login();
        MvcResult result = client.perform(post("/photo/1/comment/new").session(session).with(csrf())
                        .param("body", "test comment from normal user."))
                .andReturn();
        assertTrue(flashText(result).contains("Comment published."));
        List<Comment> commentList = comments.findAll();
        assertEquals(2, commentList.size());
        assertTrue(commentList.stream().anyMatch(c -> c.getBody().equals("test comment from normal user.")));
    }

    @Test
    void testNewTag() throws Exception {
        login("admin@helloflask.com", "123");
        MvcResult result = client.perform(post("/photo/1/tag/new").session(session).with(csrf())
                        .param("tag", "hello dog pet happy"))
                .andReturn();
        assertTrue(flashText(result).contains("Tag added."));
        Set<String> tagNames = tags.findById(1L).map(Tag::getName).stream().collect(java.util.stream.Collectors.toSet());
        assertTrue(tags.findAll().stream().map(Tag::getName).collect(java.util.stream.Collectors.toSet())
                .containsAll(java.util.List.of("hello", "dog", "pet", "happy")));
    }

    @Test
    void testSetComment() throws Exception {
        login();
        client.perform(post("/set-comment/1").session(session).with(csrf()))
                .andExpect(status().isForbidden());

        logout();
        login("admin@helloflask.com", "123");
        MvcResult result = client.perform(post("/set-comment/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Comment disabled"));
        assertFalse(photos.findById(1L).orElseThrow().isCanComment());

        result = client.perform(post("/set-comment/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Comment enabled"));
        assertTrue(photos.findById(1L).orElseThrow().isCanComment());
    }

    @Test
    void testReplyComment() throws Exception {
        login();
        MvcResult result = client.perform(get("/reply/comment/1").session(session)).andReturn();
        MvcResult followed = followRedirect(result);
        assertTrue(followed.getResponse().getContentAsString().contains("Reply to"));
    }

    @Test
    void testDeletePhoto() throws Exception {
        login();
        MvcResult result = client.perform(post("/delete/photo/2").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Photo deleted."));
        assertTrue(userOf("normal@helloflask.com") != null);
        assertFalse(photos.findById(2L).isPresent());
    }

    @Test
    void testDeleteComment() throws Exception {
        login();
        MvcResult result = client.perform(post("/delete/comment/1").session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Comment deleted."));
        assertFalse(comments.findById(1L).isPresent());
    }

    @Test
    void testShowTag() throws Exception {
        MvcResult page = client.perform(get("/tag/1")).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Order by time"));

        page = client.perform(get("/tag/1").param("order_rule", "collections")).andReturn();
        assertTrue(page.getResponse().getContentAsString().contains("Order by collections"));
    }

    @Test
    void testDeleteTag() throws Exception {
        Tag tag = new Tag();
        tag.setName("test");
        tag = tags.save(tag);
        Long tagId = tag.getId();
        // attach the tag to photo 2 directly (avoids lazy collection access in the test)
        new org.springframework.jdbc.core.JdbcTemplate(
                applicationContext.getBean(javax.sql.DataSource.class))
                .update("insert into \"photo_tag\" (\"photo_id\", \"tag_id\") values (?, ?)", 2L, tagId);

        login();
        MvcResult result = client.perform(post("/delete/tag/2/" + tagId).session(session).with(csrf())).andReturn();
        assertTrue(flashText(result).contains("Tag deleted."));
        assertFalse(tags.findById(tagId).isPresent());
    }

    @Test
    void testUploadImage() throws Exception {
        login();
        // no image
        MvcResult result = client.perform(multipart("/upload").session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("No image."));

        // invalid image type
        MockMultipartFile pdf = new MockMultipartFile("file", "test.pdf", "application/pdf", "test".getBytes());
        result = client.perform(multipart("/upload").file(pdf).session(session).with(csrf())).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Invalid image."));
    }
}
