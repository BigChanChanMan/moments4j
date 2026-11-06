package moments4j.repo;

import moments4j.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    @Query("select f from Follow f where f.followed.id = :userId order by f.createdAt desc")
    Page<Follow> findFollowers(@Param("userId") Long userId, Pageable pageable);

    @Query("select f from Follow f where f.follower.id = :userId order by f.createdAt desc")
    Page<Follow> findFollowing(@Param("userId") Long userId, Pageable pageable);

    long countByFollowedId(Long followedId);

    long countByFollowerId(Long followerId);

    @Query("select f.followed.id from Follow f where f.follower.id = :id")
    java.util.List<Long> findFollowedIds(@Param("id") Long id);
}
