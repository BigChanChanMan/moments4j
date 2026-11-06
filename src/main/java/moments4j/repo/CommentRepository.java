package moments4j.repo;

import moments4j.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPhotoIdOrderByCreatedAtAsc(Long photoId, Pageable pageable);

    long countByPhotoId(Long photoId);

    long countByFlagGreaterThan(int flag);
}
