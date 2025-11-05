package com.project.syncly.domain.note.controller;

import com.project.syncly.domain.note.dto.NoteRequestDto;
import com.project.syncly.domain.note.dto.NoteResponseDto;
import com.project.syncly.domain.note.scheduler.NoteAutoSaveScheduler;
import com.project.syncly.domain.note.service.NoteService;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces/{workspaceId}/notes")
@Tag(name = "Note API", description = "실시간 협업 노트 API")
public class NoteController {

    private final NoteService noteService;
    private final NoteAutoSaveScheduler autoSaveScheduler;

    @PostMapping
    @Operation(
            summary = "노트 생성",
            description = "워크스페이스에 새로운 노트를 생성합니다. 생성자는 자동으로 현재 인증된 사용자로 설정됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "노트 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (제목 누락 등)"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<NoteResponseDto.Create>> createNote(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Valid @RequestBody NoteRequestDto.Create requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        NoteResponseDto.Create response = noteService.createNote(workspaceId, requestDto, memberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, response));
    }

    @GetMapping
    @Operation(
            summary = "노트 목록 조회",
            description = """
                    워크스페이스의 노트 목록을 조회합니다.

                    **페이징 옵션:**
                    - page: 페이지 번호 (1부터 시작)
                    - size: 페이지당 항목 수
                    - sort: 정렬 기준 (예: lastModifiedAt,desc)

                    **응답 데이터:**
                    - 노트 목록과 페이징 정보 포함
                    - 각 노트의 작성자 정보, 참여자 수 포함
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 목록 조회 성공"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<NoteResponseDto.NoteList>> getNoteList(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "lastModifiedAt") String sortBy,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        NoteResponseDto.NoteList response = noteService.getNoteList(workspaceId, memberId, pageable);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    @GetMapping("/{noteId}")
    @Operation(
            summary = "노트 상세 조회",
            description = """
                    노트의 상세 정보를 조회합니다.

                    **응답 데이터:**
                    - 노트 내용, 작성자 정보
                    - 현재 접속 중인 참여자 목록
                    - 마지막 수정 시간
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 상세 조회 성공"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<NoteResponseDto.Detail>> getNoteDetail(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "노트 ID") @PathVariable Long noteId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        NoteResponseDto.Detail response = noteService.getNoteDetail(workspaceId, noteId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    @DeleteMapping("/{noteId}")
    @Operation(
            summary = "노트 삭제",
            description = "노트를 소프트 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<NoteResponseDto.Delete>> deleteNote(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "노트 ID") @PathVariable Long noteId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        NoteResponseDto.Delete response = noteService.deleteNote(workspaceId, noteId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    @PatchMapping("/{noteId}/title")
    @Operation(
            summary = "노트 제목 수정",
            description = "노트의 제목을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제목 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (제목 누락 등)"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<NoteResponseDto.UpdateTitleResponse>> updateNoteTitle(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "노트 ID") @PathVariable Long noteId,
            @Valid @RequestBody NoteRequestDto.UpdateTitle requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        NoteResponseDto.UpdateTitleResponse response = noteService.updateNoteTitle(workspaceId, noteId, requestDto, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    @PostMapping("/{noteId}/save")
    @Operation(
            summary = "노트 수동 저장",
            description = """
                    사용자가 명시적으로 저장 버튼을 클릭했을 때 호출합니다. (Yjs CRDT 기반)

                    **처리 흐름:**
                    1. Redis에서 현재 ydocBinary 조회
                    2. DB의 Note 엔티티에 ydocBinary 저장
                    3. Redis dirty 플래그 false로 변경
                    4. WebSocket으로 저장 완료 메시지 브로드캐스트

                    **주의사항:**
                    - Yjs CRDT 기반이므로 revision 개념 없음 (자동 충돌 해결)
                    - 자동 저장과 동일한 로직 사용
                    - 독립적인 트랜잭션으로 처리
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "Redis에 데이터가 없음"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<NoteResponseDto.SaveResponse>> saveNote(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "노트 ID") @PathVariable Long noteId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        // 권한 확인 (워크스페이스 멤버 여부만 확인)
        noteService.validateWorkspaceMember(workspaceId, memberId);

        // 수동 저장 실행
        boolean saved = autoSaveScheduler.saveNoteManually(noteId);

        NoteResponseDto.SaveResponse response;
        if (saved) {
            // ✅ Yjs CRDT 기반이므로 revision 필드 없음 (자동 충돌 해결)
            response = NoteResponseDto.SaveResponse.success(java.time.LocalDateTime.now());
        } else {
            response = NoteResponseDto.SaveResponse.failure("저장에 실패했습니다");
        }

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }
}
