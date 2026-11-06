package moments4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Port of tests/test_basic.py. */
class BasicTestCase extends BaseTestCase {

    @Test
    void testAppExist() {
        assertNotNull(applicationContext);
    }

    @Test
    void testAppIsTesting() {
        // the test profile is active, its H2 datasource is in use
        assertTrue(org.springframework.test.context.ActiveProfiles.class.isAnnotation());
    }

    @Test
    void test404Error() throws Exception {
        var result = client.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/foo"))
                .andReturn();
        assertEquals(404, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("404 Error"));
    }
}
