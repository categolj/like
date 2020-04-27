package am.ik.blog.like;

import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Repository
public class LikeRepository {
    private final DatabaseClient databaseClient;

    public LikeRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Long> countByEntryId(Long entryId) {
        return this.databaseClient.execute("SELECT COUNT(ip_address) FROM `like` WHERE entry_id = :entryId")
                .bind("entryId", entryId)
                .as(Long.class)
                .fetch()
                .one();
    }

    public Mono<Boolean> existsByEntryIdAndIpAddress(Long entryId, String ipAddress) {
        return this.databaseClient.execute("SELECT COUNT(ip_address) FROM `like` WHERE entry_id = :entryId AND ip_address = :ipAddress")
                .bind("entryId", entryId)
                .bind("ipAddress", ipAddress)
                .as(Long.class)
                .fetch()
                .one()
                .map(c -> c > 0);
    }

    @Transactional
    public Mono<Like> save(Like like) {
        return this.databaseClient.insert()
                .into(Like.class)
                .using(like)
                .then()
                .thenReturn(like);
    }
}
