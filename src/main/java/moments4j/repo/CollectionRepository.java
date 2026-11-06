package moments4j.repo;

import moments4j.model.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Optional<Collection> findByUserIdAndPhotoId(Long userId, Long photoId);

    @Query("select c from Collection c where c.photo.id = :photoId order by c.createdAt desc")
    Page<Collection> findCollectors(@Param("photoId") Long photoId, Pageable pageable);

    @Query("select c from Collection c where c.user.id = :userId order by c.createdAt desc")
    Page<Collection> findUserCollections(@Param("userId") Long userId, Pageable pageable);

    long countByPhotoId(Long photoId);

    long countByUserId(Long userId);
}
