package am.ik.blog.like;

import is.tagomor.woothee.Classifier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "like")
@CrossOrigin
public class LikeController {
    private final LikeRepository likeRepository;

    public LikeController(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @GetMapping(path = "{entryId}")
    public Mono<Map<String, Object>> getLike(@PathVariable("entryId") Long entryId,
                                             @RequestHeader(name = HttpHeaders.USER_AGENT) String userAgent,
                                             ServerWebExchange exchange) {
        final String ipAddress = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse("0.0.0.0");
        if (isBlocked(userAgent, ipAddress)) {
            return Mono.just(Map.of("count", 0, "exists", true));
        }

        final Mono<Long> countMono = this.likeRepository.countByEntryId(entryId);
        final Mono<Boolean> existsMono = this.likeRepository.existsByEntryIdAndIpAddress(entryId, ipAddress);
        return countMono.zipWith(existsMono)
                .map(tpl -> Map.of("count", tpl.getT1(), "exists", tpl.getT2()));
    }

    @PostMapping(path = "/{entryId}")
    public Mono<Like> postLike(@PathVariable("entryId") Long entryId,
                               @RequestHeader(name = HttpHeaders.USER_AGENT) String userAgent,
                               ServerWebExchange exchange) {
        final String ipAddress = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse("0.0.0.0");
        final Like like = new Like(UUID.randomUUID().toString(), entryId, LocalDateTime.now(), ipAddress);
        if (isBlocked(userAgent, ipAddress)) {
            return Mono.just(like);
        }
        return this.likeRepository.save(like);
    }

    static boolean isBlocked(String userAgent, String ipAddress) {
        final boolean isBrowser = Classifier.tryBrowser(userAgent, Map.of());
        return !(isBrowser || "127.0.0.1".equals(ipAddress));
    }
}
