package am.ik.blog.like;

import is.tagomor.woothee.Classifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "likes")
@CrossOrigin
public class LikeController {
    private final LikeRepository likeRepository;

    public LikeController(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @GetMapping(path = "")
    public Flux<Like> getLikes() {
        return this.likeRepository.findOrderByLikeAtDesc();
    }

    @GetMapping(path = "{entryId}")
    public Mono<Map<String, Object>> getLike(@PathVariable("entryId") Long entryId,
                                             @RequestHeader(name = HttpHeaders.USER_AGENT) String userAgent,
                                             ServerWebExchange exchange) {
        final String ipAddress = getIpAddress(exchange);
        if (isBlocked(userAgent, ipAddress)) {
            return Mono.just(Map.of("count", 0, "exists", true));
        }

        final Mono<Long> countMono = this.likeRepository.countByEntryId(entryId);
        final Mono<Boolean> existsMono = this.likeRepository.existsByEntryIdAndIpAddress(entryId, ipAddress);
        return countMono.zipWith(existsMono)
                .map(tpl -> Map.of("count", tpl.getT1(), "exists", tpl.getT2()));
    }

    @PostMapping(path = "{entryId}")
    public Mono<Like> postLike(@PathVariable("entryId") Long entryId,
                               @RequestHeader(name = HttpHeaders.USER_AGENT) String userAgent,
                               ServerWebExchange exchange) {
        final String ipAddress = getIpAddress(exchange);
        final Like like = new Like(UUID.randomUUID().toString(), entryId, LocalDateTime.now(), ipAddress);
        if (isBlocked(userAgent, ipAddress)) {
            return Mono.just(like);
        }
        return this.likeRepository.save(like);
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
