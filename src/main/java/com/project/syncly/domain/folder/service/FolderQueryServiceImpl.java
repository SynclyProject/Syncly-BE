package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.dto.ListingDto;
import com.project.syncly.domain.folder.dto.PermissionDto;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.exception.FolderErrorCode;
import com.project.syncly.domain.folder.exception.FolderException;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.folder.repository.FolderClosureRepository;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderQueryServiceImpl implements FolderQueryService {

    private final FolderRepository folderRepository;
    private final FolderClosureRepository folderClosureRepository;
    private final WorkspaceRepository workspaceRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Override
    public FolderResponseDto.Root getRootFolder(Long workspaceId) {
        // 워크스페이스 유효성 검증
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new FolderException(FolderErrorCode.WORKSPACE_NOT_FOUND);
        }

        // 루트 폴더 조회
        Folder rootFolder = folderRepository.findByWorkspaceIdAndParentIdIsNull(workspaceId)
                .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));

        return new FolderResponseDto.Root(
                rootFolder.getId(),
                rootFolder.getWorkspaceId(),
                rootFolder.getName(),
                rootFolder.getCreatedAt()
        );
    }

    @Override
    public FolderResponseDto.Path getFolderPath(Long workspaceId, Long folderId) {
        // 폴더가 해당 워크스페이스에 속하는지 확인
        if (!folderRepository.existsByWorkspaceIdAndId(workspaceId, folderId)) {
            throw new FolderException(FolderErrorCode.FOLDER_NOT_FOUND);
        }

        // FolderClosure 테이블을 활용해 루트부터 현재 폴더까지의 전체 경로 조회
        List<Object[]> pathData = folderClosureRepository.findPathFromRoot(folderId);

        // Object[] 배열을 FolderResponseDto.PathItem으로 변환
        List<FolderResponseDto.PathItem> pathItems = pathData.stream()
                .map(row -> new FolderResponseDto.PathItem(
                        (Long) row[0],   // id
                        (String) row[1]  // name
                ))
                .collect(Collectors.toList());

        return new FolderResponseDto.Path(pathItems);
    }

    @Override
    public FolderResponseDto.ItemList getFolderItems(Long workspaceId, Long folderId, String sort, String cursor, Integer limit, String search, Long uploaderId) {
        // 폴더가 해당 워크스페이스에 속하는지 확인
        if (!folderRepository.existsByWorkspaceIdAndId(workspaceId, folderId)) {
            throw new FolderException(FolderErrorCode.FOLDER_NOT_FOUND);
        }

        // 기본값 설정
        if (sort == null) sort = "latest";
        if (limit == null) limit = 20;
        if (limit > 100) limit = 100;

        List<ListingDto.Item> items = new ArrayList<>();
        String nextCursor = null;

        // 커서 기반 페이징 처리
        if (cursor != null && !cursor.isEmpty()) {
            items = getFolderItemsWithCursor(folderId, sort, cursor, limit, search, uploaderId);
        } else {
            items = getFolderItemsWithoutCursor(folderId, sort, limit, search, uploaderId);
        }

        // nextCursor 계산
        if (items.size() == limit) {
            ListingDto.Item lastItem = items.get(items.size() - 1);
            if ("latest".equals(sort)) {
                // 최신순의 경우 날짜를 기준으로 커서 생성
                nextCursor = lastItem.date().replace(".", "-") + "T23:59:59";
            } else if ("alphabet".equals(sort)) {
                // 가나다순의 경우 이름을 기준으로 커서 생성
                nextCursor = lastItem.name();
            }
        }

        // 권한 정보 생성 (워크스페이스 멤버라면 모든 권한 true)
        PermissionDto permissions = new PermissionDto(true, true, true, true);

        return new FolderResponseDto.ItemList(items, nextCursor, permissions);
    }

    private List<ListingDto.Item> getFolderItemsWithCursor(Long folderId, String sort, String cursor, Integer limit, String search, Long uploaderId) {
        List<ListingDto.Item> items = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, limit);

        if ("latest".equals(sort)) {
            LocalDateTime cursorDateTime = LocalDateTime.parse(cursor.replace(".000Z", ""));

            // 폴더 조회 (latest 정렬)
            List<Object[]> folders = folderRepository.findSubFoldersByParentIdWithCursorLatest(folderId, search, uploaderId, cursorDateTime, pageable);
            // 파일 조회 (latest 정렬)
            List<Object[]> files = folderRepository.findFilesByFolderIdWithCursorLatest(folderId, search, uploaderId, cursorDateTime, pageable);

            // 폴더 데이터 변환
            for (Object[] row : folders) {
                items.add(convertFolderRowToItem(row));
            }

            // 파일 데이터 변환
            for (Object[] row : files) {
                items.add(convertFileRowToItem(row));
            }

            // 수정일시 기준 내림차순 정렬
            items.sort((a, b) -> b.date().compareTo(a.date()));

        } else if ("alphabet".equals(sort)) {
            // 폴더 조회 (alphabet 정렬)
            List<Object[]> folders = folderRepository.findSubFoldersByParentIdWithCursorAlphabet(folderId, search, uploaderId, cursor, pageable);
            // 파일 조회 (alphabet 정렬)
            List<Object[]> files = folderRepository.findFilesByFolderIdWithCursorAlphabet(folderId, search, uploaderId, cursor, pageable);

            // 폴더 데이터 변환
            for (Object[] row : folders) {
                items.add(convertFolderRowToItem(row));
            }

            // 파일 데이터 변환
            for (Object[] row : files) {
                items.add(convertFileRowToItem(row));
            }

            // 이름 기준 오름차순 정렬
            items.sort(Comparator.comparing(ListingDto.Item::name));
        }

        // limit만큼 자르기
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        return items;
    }

    private List<ListingDto.Item> getFolderItemsWithoutCursor(Long folderId, String sort, Integer limit, String search, Long uploaderId) {
        List<ListingDto.Item> items = new ArrayList<>();

        // 폴더 조회
        List<Object[]> folders = folderRepository.findSubFoldersByParentIdWithMember(folderId, search, sort, uploaderId);
        // 파일 조회
        List<Object[]> files = folderRepository.findFilesByFolderIdWithMember(folderId, search, sort, uploaderId);

        // 폴더 데이터 변환
        for (Object[] row : folders) {
            items.add(convertFolderRowToItem(row));
        }

        // 파일 데이터 변환
        for (Object[] row : files) {
            items.add(convertFileRowToItem(row));
        }

        // 정렬
        if ("latest".equals(sort)) {
            items.sort((a, b) -> b.date().compareTo(a.date()));
        } else if ("alphabet".equals(sort)) {
            items.sort(Comparator.comparing(ListingDto.Item::name));
        }

        // limit만큼 자르기
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        return items;
    }

    private ListingDto.Item convertFolderRowToItem(Object[] row) {
        Long id = (Long) row[0];
        String name = (String) row[1];
        LocalDateTime createdAt = (LocalDateTime) row[2];
        LocalDateTime updatedAt = (LocalDateTime) row[3];
        Long wmId = (Long) row[4];
        String wmName = (String) row[5];
        String wmProfileImage = (String) row[6];

        // 날짜 포맷팅 (수정일시 우선, 없으면 생성일시)
        String formattedDate = (updatedAt != null ? updatedAt : createdAt).format(DATE_FORMATTER);

        return new ListingDto.Item(
            id,
            "FOLDER",
            name,
            formattedDate,
            new ListingDto.UserInfo(wmId, wmName, wmProfileImage)
        );
    }

    private ListingDto.Item convertFileRowToItem(Object[] row) {
        Long id = (Long) row[0];
        String name = (String) row[1];
        LocalDateTime createdAt = (LocalDateTime) row[2];
        LocalDateTime updatedAt = (LocalDateTime) row[3];
        Long size = (Long) row[4];
        String objectKey = (String) row[5];
        String type = (String) row[6];
        Long wmId = (Long) row[7];
        String wmName = (String) row[8];
        String wmProfileImage = (String) row[9];

        // 날짜 포맷팅 (수정일시 우선, 없으면 생성일시)
        String formattedDate = (updatedAt != null ? updatedAt : createdAt).format(DATE_FORMATTER);

        return new ListingDto.Item(
            id,
            "FILE",
            name,
            formattedDate,
            new ListingDto.UserInfo(wmId, wmName, wmProfileImage)
        );
    }

    @Override
    public FolderResponseDto.ItemList getTrashItems(Long workspaceId, String sort, String cursor, Integer limit, String search, Long uploaderId) {
        // 워크스페이스 유효성 검증
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new FolderException(FolderErrorCode.WORKSPACE_NOT_FOUND);
        }

        // 기본값 설정
        if (sort == null) sort = "latest";
        if (limit == null) limit = 20;
        if (limit > 100) limit = 100;

        List<ListingDto.Item> items = new ArrayList<>();
        String nextCursor = null;

        // 커서 기반 페이징 처리
        if (cursor != null && !cursor.isEmpty()) {
            items = getTrashItemsWithCursor(workspaceId, sort, cursor, limit, search, uploaderId);
        } else {
            items = getTrashItemsWithoutCursor(workspaceId, sort, limit, search, uploaderId);
        }

        // nextCursor 계산
        if (items.size() == limit) {
            ListingDto.Item lastItem = items.get(items.size() - 1);
            if ("latest".equals(sort)) {
                // 최신순의 경우 날짜를 기준으로 커서 생성
                nextCursor = lastItem.date().replace(".", "-") + "T23:59:59";
            } else if ("alphabet".equals(sort)) {
                // 가나다순의 경우 이름을 기준으로 커서 생성
                nextCursor = lastItem.name();
            }
        }

        // 권한 정보 생성 (워크스페이스 멤버라면 모든 권한 true)
        PermissionDto permissions = new PermissionDto(true, true, true, true);

        return new FolderResponseDto.ItemList(items, nextCursor, permissions);
    }

    private List<ListingDto.Item> getTrashItemsWithCursor(Long workspaceId, String sort, String cursor, Integer limit, String search, Long uploaderId) {
        List<ListingDto.Item> items = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, limit);

        if ("latest".equals(sort)) {
            LocalDateTime cursorDateTime = LocalDateTime.parse(cursor.replace(".000Z", ""));

            // 휴지통 폴더 조회 (latest 정렬)
            List<Object[]> folders = folderRepository.findTrashFoldersByWorkspaceIdWithCursorLatest(workspaceId, search, uploaderId, cursorDateTime, pageable);
            // 휴지통 파일 조회 (latest 정렬)
            List<Object[]> files = folderRepository.findTrashFilesByWorkspaceIdWithCursorLatest(workspaceId, search, uploaderId, cursorDateTime, pageable);

            // 폴더 데이터 변환 (deletedAt 기준)
            for (Object[] row : folders) {
                items.add(convertTrashFolderRowToItem(row));
            }

            // 파일 데이터 변환 (deletedAt 기준)
            for (Object[] row : files) {
                items.add(convertTrashFileRowToItem(row));
            }

            // 삭제일시 기준 내림차순 정렬
            items.sort((a, b) -> b.date().compareTo(a.date()));

        } else if ("alphabet".equals(sort)) {
            // 휴지통 폴더 조회 (alphabet 정렬)
            List<Object[]> folders = folderRepository.findTrashFoldersByWorkspaceIdWithCursorAlphabet(workspaceId, search, uploaderId, cursor, pageable);
            // 휴지통 파일 조회 (alphabet 정렬)
            List<Object[]> files = folderRepository.findTrashFilesByWorkspaceIdWithCursorAlphabet(workspaceId, search, uploaderId, cursor, pageable);

            // 폴더 데이터 변환
            for (Object[] row : folders) {
                items.add(convertTrashFolderRowToItem(row));
            }

            // 파일 데이터 변환
            for (Object[] row : files) {
                items.add(convertTrashFileRowToItem(row));
            }

            // 이름 기준 오름차순 정렬
            items.sort(Comparator.comparing(ListingDto.Item::name));
        }

        // limit만큼 자르기
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        return items;
    }

    private List<ListingDto.Item> getTrashItemsWithoutCursor(Long workspaceId, String sort, Integer limit, String search, Long uploaderId) {
        List<ListingDto.Item> items = new ArrayList<>();

        // 휴지통 폴더 조회
        List<Object[]> folders = folderRepository.findTrashFoldersByWorkspaceIdWithMember(workspaceId, search, sort, uploaderId);
        // 휴지통 파일 조회
        List<Object[]> files = folderRepository.findTrashFilesByWorkspaceIdWithMember(workspaceId, search, sort, uploaderId);

        // 폴더 데이터 변환
        for (Object[] row : folders) {
            items.add(convertTrashFolderRowToItem(row));
        }

        // 파일 데이터 변환
        for (Object[] row : files) {
            items.add(convertTrashFileRowToItem(row));
        }

        // 정렬
        if ("latest".equals(sort)) {
            items.sort((a, b) -> b.date().compareTo(a.date()));
        } else if ("alphabet".equals(sort)) {
            items.sort(Comparator.comparing(ListingDto.Item::name));
        }

        // limit만큼 자르기
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        return items;
    }

    private ListingDto.Item convertTrashFolderRowToItem(Object[] row) {
        Long id = (Long) row[0];
        String name = (String) row[1];
        LocalDateTime createdAt = (LocalDateTime) row[2];
        LocalDateTime updatedAt = (LocalDateTime) row[3];
        Long wmId = (Long) row[4];
        String wmName = (String) row[5];
        String wmProfileImage = (String) row[6];

        // 날짜 포맷팅 (수정일시 우선, 없으면 생성일시)
        String formattedDate = (updatedAt != null ? updatedAt : createdAt).format(DATE_FORMATTER);

        return new ListingDto.Item(
            id,
            "FOLDER",
            name,
            formattedDate,
            new ListingDto.UserInfo(wmId, wmName, wmProfileImage)
        );
    }

    private ListingDto.Item convertTrashFileRowToItem(Object[] row) {
        Long id = (Long) row[0];
        String name = (String) row[1];
        LocalDateTime createdAt = (LocalDateTime) row[2];
        LocalDateTime updatedAt = (LocalDateTime) row[3];
        Long size = (Long) row[4];
        String objectKey = (String) row[5];
        String type = (String) row[6];
        Long wmId = (Long) row[7];
        String wmName = (String) row[8];
        String wmProfileImage = (String) row[9];

        // 날짜 포맷팅 (수정일시 우선, 없으면 생성일시)
        String formattedDate = (updatedAt != null ? updatedAt : createdAt).format(DATE_FORMATTER);

        return new ListingDto.Item(
            id,
            "FILE",
            name,
            formattedDate,
            new ListingDto.UserInfo(wmId, wmName, wmProfileImage)
        );
    }
}