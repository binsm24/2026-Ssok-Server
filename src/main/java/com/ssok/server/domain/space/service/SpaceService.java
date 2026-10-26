package com.ssok.server.domain.space.service;

import com.ssok.server.common.exception.ForbiddenException;
import com.ssok.server.common.exception.InvalidRequestException;
import com.ssok.server.common.exception.SpaceNotFoundException;
import com.ssok.server.common.exception.UnauthenticatedException;
import com.ssok.server.common.util.TimeFormatter;
import com.ssok.server.domain.bookmark.repository.BookmarkRepository;
import com.ssok.server.domain.space.dto.SpaceCreateRequest;
import com.ssok.server.domain.space.dto.SpaceCreateResponse;
import com.ssok.server.domain.space.dto.SpaceDetailResponse;
import com.ssok.server.domain.space.dto.SpaceSummaryDto;
import com.ssok.server.domain.space.entity.Space;
import com.ssok.server.domain.space.entity.SpaceMember;
import com.ssok.server.domain.space.entity.SpaceRole;
import com.ssok.server.domain.space.entity.SpaceType;
import com.ssok.server.domain.space.repository.SpaceMemberRepository;
import com.ssok.server.domain.space.repository.SpaceRepository;
import com.ssok.server.domain.user.entity.User;
import com.ssok.server.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    @Transactional
    public SpaceCreateResponse create(Long userId, SpaceCreateRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthenticatedException("authentication required"));

        SpaceType type = parseType(request.type());

        Space space = Space.builder()
                .name(request.spaceName())
                .description(request.description())
                .type(type)
                .owner(owner)
                .build();
        Space saved = spaceRepository.save(space);

        SpaceMember ownerMembership = SpaceMember.builder()
                .space(saved)
                .user(owner)
                .role(SpaceRole.OWNER)
                .build();
        spaceMemberRepository.save(ownerMembership);

        return new SpaceCreateResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getType().name());
    }

    @Transactional(readOnly = true)
    public List<SpaceSummaryDto> list(Long userId) {
        return spaceMemberRepository.findAllByUserId(userId).stream()
                .map(SpaceMember::getSpace)
                .map(space -> new SpaceSummaryDto(
                        space.getId(), space.getName(), space.getType().name(),
                        bookmarkRepository.countBySpaceId(space.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaceDetailResponse getDetail(Long userId, Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new SpaceNotFoundException("space not found"));

        spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() ->
                        new ForbiddenException("space access denied"));

        long memberCount = spaceMemberRepository.countBySpaceId(spaceId);
        long bookmarkCount = bookmarkRepository.countBySpaceId(spaceId);

        return new SpaceDetailResponse(
                space.getId(),
                space.getName(),
                space.getDescription(),
                space.getType().name(),
                space.getOwner().getId(),
                memberCount,
                bookmarkCount,
                TimeFormatter.format(space.getCreatedAt())
        );
    }

    @Transactional
    public void delete(Long userId, Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() ->
                        new SpaceNotFoundException("space not found"));

        SpaceMember requester = spaceMemberRepository
                .findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() ->
                        new ForbiddenException("space access denied"));

        if (requester.getRole() != SpaceRole.OWNER) {
            throw new ForbiddenException(
                    "only owner can delete the space");
        }

        // 엔티티 삭제로 members/bookmarks에 REMOVE를 전파하고,
        // Bookmark의 ElementCollection인 bookmark_tags도 같은 트랜잭션에서 삭제한다.
        // 벌크 DELETE는 JPA 연쇄 삭제를 생략하므로 사용하지 않는다.
        spaceRepository.delete(space);
    }

    private SpaceType parseType(String type) {
        try {
            return SpaceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("invalid request");
        }
    }
}
