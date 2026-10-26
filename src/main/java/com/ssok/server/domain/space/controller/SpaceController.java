package com.ssok.server.domain.space.controller;

import com.ssok.server.common.exception.UnauthenticatedException;
import com.ssok.server.common.response.ApiResponse;
import com.ssok.server.domain.space.dto.MemberDto;
import com.ssok.server.domain.space.dto.MemberInviteRequest;
import com.ssok.server.domain.space.dto.MemberInviteResponse;
import com.ssok.server.domain.space.dto.SpaceCreateRequest;
import com.ssok.server.domain.space.dto.SpaceCreateResponse;
import com.ssok.server.domain.space.dto.SpaceDetailResponse;
import com.ssok.server.domain.space.dto.SpaceSummaryDto;
import com.ssok.server.domain.space.service.SpaceMemberService;
import com.ssok.server.domain.space.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "스페이스", description = "스페이스 관련 API")
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final SpaceMemberService spaceMemberService;

    @Operation(summary = "스페이스 생성", description = "새로운 스페이스 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<SpaceCreateResponse>> create(
            Authentication authentication, @RequestBody @Valid SpaceCreateRequest request) {
        SpaceCreateResponse response = spaceService.create(requireUserId(authentication), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "space created successfully", response));
    }

    @Operation(summary = "스페이스 목록", description = "사용자가 생성하거나 참여 중인 스페이스 목록을 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SpaceSummaryDto>>> list(Authentication authentication) {
        List<SpaceSummaryDto> response = spaceService.list(requireUserId(authentication));

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "space list retrieved successfully", response));
    }

    @Operation(summary = "스페이스 조회", description = "선택한 스페이스의 상세 정보 조회")
    @GetMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceDetailResponse>> getDetail(
            Authentication authentication,
            @PathVariable Long spaceId) {

        SpaceDetailResponse response =
                spaceService.getDetail(requireUserId(authentication), spaceId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "space retrieved successfully",
                        response
                )
        );
    }

    @Operation(summary = "스페이스 삭제", description = "OWNER만 삭제 가능. 해당 스페이스의 북마크, 북마크 태그 및 멤버 정보를 DB에서 함께 영구 삭제")
    @DeleteMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<Boolean>> delete(
            Authentication authentication,
            @PathVariable Long spaceId
    ) {
        spaceService.delete(requireUserId(authentication), spaceId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "space deleted successfully",
                        true
                )
        );
    }

    @Operation(summary = "팀원 초대", description = "팀 스페이스에 새로운 팀원 초대")
    @PostMapping("/{spaceId}/invite")
    public ResponseEntity<ApiResponse<MemberInviteResponse>> invite(
            Authentication authentication,
            @PathVariable Long spaceId,
            @RequestBody @Valid MemberInviteRequest request) {

        MemberInviteResponse response =
                spaceMemberService.invite(
                        requireUserId(authentication),
                        spaceId,
                        request.email()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "member invited successfully",
                        response
                ));
    }

    @Operation(summary = "팀원 목록", description = "팀 스페이스에 참여 중인 팀원 목록 조회")
    @GetMapping("/{spaceId}/members")
    public ResponseEntity<ApiResponse<List<MemberDto>>> listMembers(
            Authentication authentication,
            @PathVariable Long spaceId
    ) {
        List<MemberDto> response =
                spaceMemberService.list(
                        requireUserId(authentication),
                        spaceId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "member list retrieved successfully",
                        response
                )
        );
    }

    @Operation(summary = "팀원 삭제", description = "팀 스페이스에서 팀원 제거")
    @DeleteMapping("/{spaceId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Boolean>> removeMember(
            Authentication authentication,
            @PathVariable Long spaceId,
            @PathVariable Long memberId
    ) {
        spaceMemberService.remove(
                requireUserId(authentication),
                spaceId,
                memberId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "member removed successfully",
                        true
                )
        );
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthenticatedException("authentication required");
        }

        return (Long) authentication.getPrincipal();
    }
}
