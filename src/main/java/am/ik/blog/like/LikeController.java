package am.ik.blog.like;

import am.ik.blog.like.spec.LikeCountResponse;
import am.ik.blog.like.spec.LikeResponse;
import am.ik.blog.like.spec.LikesApi;
import is.tagomor.woothee.Classifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin
public class LikeController implements LikesApi {
    private final LikeRepository likeRepository;

    public LikeController(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @Override
    public Mono<ResponseEntity<Flux<LikeResponse>>> getLikes(ServerWebExchange exchange) {
        final Flux<LikeResponse> likes = this.likeRepository.findOrderByLikeAtDesc()
                .map(this::toSpec);
        return Mono.just(ResponseEntity.ok(likes));
    }

    @Override
    public Mono<ResponseEntity<LikeCountResponse>> getLike(Long entryId, ServerWebExchange exchange) {
        final String ipAddress = getIpAddress(exchange);
        final String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        if (isBlocked(userAgent, ipAddress)) {
            return Mono.just(ResponseEntity.ok(new LikeCountResponse().count(0L).exists(true)));
        }

        final Mono<Long> countMono = this.likeRepository.countByEntryId(entryId);
        final Mono<Boolean> existsMono = this.likeRepository.existsByEntryIdAndIpAddress(entryId, ipAddress);
        return countMono.zipWith(existsMono)
                .map(tpl -> new LikeCountResponse().count(tpl.getT1()).exists(tpl.getT2()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity.notFound().build()));
    }

    @Override
    public Mono<ResponseEntity<LikeResponse>> postLike(Long entryId, ServerWebExchange exchange) {
        final String ipAddress = getIpAddress(exchange);
        final Like like = new Like(UUID.randomUUID().toString(), entryId, LocalDateTime.now(), ipAddress);
        final String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        if (isBlocked(userAgent, ipAddress)) {
            return Mono.just(ResponseEntity.ok(toSpec(like)));
        }
        return this.likeRepository.save(like)
                .map(this::toSpec)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteLike(Long entryId, ServerWebExchange exchange) {
        final String ipAddress = getIpAddress(exchange);
        return this.likeRepository.deleteByEntryIdAndIpAddress(entryId, ipAddress)
                .map(__ -> ResponseEntity.noContent().<Void>build())
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity.notFound().build()));
    }

    LikeResponse toSpec(Like like) {
        return new LikeResponse()
                .likeId(UUID.fromString(like.getLikeId()))
                .entryId(like.getEntryId())
                .likeAt(like.getLikeAt());
    }

    static String getIpAddress(ServerWebExchange exchange) {
        final ServerHttpRequest request = exchange.getRequest();
        final Optional<String> xForwardedFor = Optional.ofNullable(request.getHeaders().getFirst("X-Forwarded-For"))
                .map(x -> x.split(",")[0])
                .map(String::strip);
        return xForwardedFor.or(() -> Optional.ofNullable(request.getRemoteAddress())
                .map(InetSocketAddress::getHostString))
                .orElse("0.0.0.0");
    }

    static boolean isBlocked(String userAgent, String ipAddress) {
        final boolean isBrowser = Classifier.tryBrowser(userAgent, new HashMap<>());
        return !(isBrowser || "127.0.0.1".equals(ipAddress));
    }
}
