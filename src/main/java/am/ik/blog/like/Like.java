package am.ik.blog.like;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("`like`")
public class Like {
    @Id
    private final String likeId;
    private final Long entryId;
    private final LocalDateTime likeAt;
    private final String ipAddress;

    public Like(String likeId, Long entryId, LocalDateTime likeAt, String ipAddress) {
        this.likeId = likeId;
        this.entryId = entryId;
        this.likeAt = likeAt;
        this.ipAddress = ipAddress;
    }

    public String getLikeId() {
        return likeId;
    }

    public Long getEntryId() {
        return entryId;
    }

    public LocalDateTime getLikeAt() {
        return likeAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String toString() {
        return "Like{" +
                "likeId='" + likeId + '\'' +
                ", entryId=" + entryId +
                ", likeAt=" + likeAt +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
