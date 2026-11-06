package moments4j.repo;

import moments4j.model.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    long countByFlagGreaterThan(int flag);

    long countByAuthorId(Long authorId);

    Page<Photo> findByAuthorIdIn(List<Long> authorIds, Pageable pageable);

    Page<Photo> findByAuthorId(Long authorId, Pageable pageable);

    Page<Photo> findByTagsIdOrderByCreatedAtDesc(Long tagId, Pageable pageable);

    /** Next photo of the same author, created earlier (order desc). */
    @Query("select p from Photo p where p.author.id = :authorId and p.createdAt < :createdAt " +
            "order by p.createdAt desc")
    List<Photo> findNextOf(@Param("authorId") Long authorId, @Param("createdAt") LocalDateTime createdAt,
                           Pageable pageable);

    /** Previous photo of the same author, created later (order asc). */
    @Query("select p from Photo p where p.author.id = :authorId and p.createdAt > :createdAt " +
            "order by p.createdAt asc")
    List<Photo> findPreviousOf(@Param("authorId") Long authorId, @Param("createdAt") LocalDateTime createdAt,
                               Pageable pageable);

    // ponytail: LIKE search stands in for whooshee full-text search on description
    @Query("select p from Photo p where lower(p.description) like lower(concat('%', :q, '%'))")
    Page<Photo> search(@Param("q") String q, Pageable pageable);
}
