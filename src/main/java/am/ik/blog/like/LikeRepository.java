package am.ik.blog.like;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;

@Repository
public class LikeRepository {
    private final DatabaseClient databaseClient;

    public LikeRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Flux<Like> findOrderByLikeAtDesc() {
        return this.databaseClient.select()
                .from(Like.class)
                .orderBy(Sort.Order.desc("like_at"))
                .page(PageRequest.of(0, 20))
                .fetch()
                .all();
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

    @Transactional
    public Mono<Void> deleteByEntryIdAndIpAddress(Long entryId, String ipAddress) {
        return this.databaseClient.delete()
                .from(Like.class)
                .matching(where("entry_id").is(entryId)
                        .and("ip_address").is(ipAddress))
                .then();
    }
}
