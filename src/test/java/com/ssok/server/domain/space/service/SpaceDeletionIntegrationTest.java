package com.ssok.server.domain.space.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssok.server.common.exception.ForbiddenException;
import com.ssok.server.domain.bookmark.entity.Bookmark;
import com.ssok.server.domain.space.entity.Space;
import com.ssok.server.domain.space.entity.SpaceMember;
import com.ssok.server.domain.space.entity.SpaceRole;
import com.ssok.server.domain.space.entity.SpaceType;
import com.ssok.server.domain.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import(SpaceService.class)
class SpaceDeletionIntegrationTest {

    @Autowired private TestEntityManager entities;
    @Autowired private SpaceService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void ownerDeletesBookmarkTagsAndMembersFromDatabaseForBothSpaceTypes() {
        User owner = user("owner@test.com");
        User member = user("member@test.com");
        for (SpaceType type : SpaceType.values()) {
            Space target = space(owner, type);
            Space preserved = space(owner, type);
            membership(target, member, SpaceRole.MEMBER);
            Bookmark first = bookmark(target, "first");
            Bookmark second = bookmark(target, "second");
            Bookmark retained = bookmark(preserved, "retained");
            Long targetId = target.getId();
            Long preservedId = preserved.getId();
            List<Long> removedIds = List.of(first.getId(), second.getId());
            Long retainedId = retained.getId();
            Long ownerId = owner.getId();
            entities.flush();
            entities.clear(); // 자식 컬렉션을 로드하지 않은 상태에서 삭제한다.

            service.delete(ownerId, targetId);
            entities.flush();
            entities.clear();

            assertThat(count("spaces", "id", targetId)).isZero();
            assertThat(count("space_members", "space_id", targetId)).isZero();
            assertThat(count("bookmarks", "space_id", targetId)).isZero();
            for (Long bookmarkId : removedIds) {
                assertThat(count("bookmark_tags", "bookmark_id", bookmarkId)).isZero();
            }
            assertThat(count("spaces", "id", preservedId)).isEqualTo(1);
            assertThat(count("space_members", "space_id", preservedId)).isEqualTo(1);
            assertThat(count("bookmarks", "id", retainedId)).isEqualTo(1);
            assertThat(count("bookmark_tags", "bookmark_id", retainedId)).isEqualTo(2);
            assertThat(count("users", "id", ownerId)).isEqualTo(1);
            assertThat(count("users", "id", member.getId())).isEqualTo(1);
        }
    }

    @Test
    void memberAndNonMemberCannotDeleteAnyAssociatedRows() {
        User owner = user("owner@test.com");
        User member = user("member@test.com");
        User outsider = user("outsider@test.com");
        Space target = space(owner, SpaceType.TEAM);
        membership(target, member, SpaceRole.MEMBER);
        Bookmark bookmark = bookmark(target, "protected");
        Long spaceId = target.getId();
        Long bookmarkId = bookmark.getId();
        entities.flush();
        entities.clear();

        for (Long requesterId : List.of(member.getId(), outsider.getId())) {
            assertThatThrownBy(() -> service.delete(requesterId, spaceId))
                    .isInstanceOf(ForbiddenException.class);
            entities.flush();
            entities.clear();
            assertThat(count("spaces", "id", spaceId)).isEqualTo(1);
            assertThat(count("space_members", "space_id", spaceId)).isEqualTo(2);
            assertThat(count("bookmarks", "id", bookmarkId)).isEqualTo(1);
            assertThat(count("bookmark_tags", "bookmark_id", bookmarkId)).isEqualTo(2);
        }
    }

    @Test
    void ownerCanDeleteSpaceWithoutBookmarks() {
        User owner = user("owner@test.com");
        Space target = space(owner, SpaceType.TEAM);
        Long spaceId = target.getId();
        entities.flush();
        entities.clear();
        service.delete(owner.getId(), spaceId);
        entities.flush();
        entities.clear();
        assertThat(count("spaces", "id", spaceId)).isZero();
        assertThat(count("space_members", "space_id", spaceId)).isZero();
    }

    private User user(String email) {
        return entities.persist(User.builder().email(email).password("test-password").name("tester").build());
    }

    private Space space(User owner, SpaceType type) {
        Space space = entities.persist(Space.builder().name("space").type(type).owner(owner).build());
        membership(space, owner, SpaceRole.OWNER);
        return space;
    }

    private void membership(Space space, User user, SpaceRole role) {
        entities.persist(SpaceMember.builder().space(space).user(user).role(role).build());
    }

    private Bookmark bookmark(Space space, String title) {
        // PR #4와 동일하게 북마크의 space만 설정해 저장한다.
        return entities.persist(Bookmark.builder().space(space).url("https://example.com/" + title)
                .title(title).summary("summary").tags(new ArrayList<>(List.of("java", title))).build());
    }

    private long count(String table, String column, Long id) {
        return jdbc.queryForObject("select count(*) from " + table + " where " + column + " = ?", Long.class, id);
    }
}
