package moments4j.repo;

import moments4j.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByLocked(boolean locked);

    long countByActive(boolean active);

    Page<User> findByRoleName(String roleName, Pageable pageable);

    Page<User> findByLocked(boolean locked, Pageable pageable);

    Page<User> findByActive(boolean active, Pageable pageable);

    // ponytail: LIKE search stands in for whooshee full-text search on (name, username);
    // swap for Lucene/Hibernate Search if fuzzy matching is needed
    @Query("select u from User u where lower(u.name) like lower(concat('%', :q, '%')) " +
            "or lower(u.username) like lower(concat('%', :q, '%'))")
    Page<User> search(@Param("q") String q, Pageable pageable);
}
