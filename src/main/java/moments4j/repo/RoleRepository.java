package moments4j.repo;

import moments4j.model.Role;
import moments4j.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Permission> findPermissionByName(String name);
}
