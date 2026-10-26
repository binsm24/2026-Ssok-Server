package com.ssok.server.domain.space.entity;

import com.ssok.server.domain.bookmark.entity.Bookmark;
import com.ssok.server.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "spaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpaceMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    // 스페이스 삭제 시 북마크와 그 태그 컬렉션까지 DB에서 제거한다.
    private List<Bookmark> bookmarks = new ArrayList<>();

    @Builder
    public Space(
            String name,
            String description,
            SpaceType type,
            User owner
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.owner = owner;
    }
}
