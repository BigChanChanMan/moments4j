package moments4j.repo;

import moments4j.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    java.util.Optional<Tag> findByName(String name);

    /** Top 10 tags by photo count, mirrors the index() sidebar query. */
    @Query("select t from Tag t join t.photos p group by t.id order by count(p.id) desc")
    List<Tag> findTopTags(Pageable pageable);

    // ponytail: LIKE search stands in for whooshee full-text search on name
    @Query("select t from Tag t where lower(t.name) like lower(concat('%', :q, '%'))")
    Page<Tag> search(@Param("q") String q, Pageable pageable);
}
