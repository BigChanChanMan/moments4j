package moments4j;

import moments4j.model.Role;
import moments4j.repo.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port of tests/test_cli.py (init-db / init-app / lorem run as jar commands —
 * here the equivalent service-level operations are exercised directly).
 */
class CliTestCase extends BaseTestCase {

    @Autowired
    private RoleRepository roles;

    @Test
    void testInitAppCommand() {
        // init-app = drop all + init_role
        notifications.deleteAll();
        comments.deleteAll();
        photos.deleteAll();
        tags.deleteAll();
        users.deleteAll();
        tags.deleteAll();
        roles.deleteAll();

        userService.initRole();
        List<Role> roleList = roles.findAll();
        assertEquals(4, roleList.size());
        assertTrue(roleList.stream().anyMatch(r -> r.getName().equals("Administrator")));
        assertTrue(roleList.stream().anyMatch(r -> r.getName().equals("Moderator")));
        assertTrue(roleList.stream().anyMatch(r -> r.getName().equals("User")));
        assertTrue(roleList.stream().anyMatch(r -> r.getName().equals("Locked")));
    }
}
