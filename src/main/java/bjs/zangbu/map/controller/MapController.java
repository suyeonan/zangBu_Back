package bjs.zangbu.map.controller;

import bjs.zangbu.map.dto.request.MapCategoryRequest;
import bjs.zangbu.map.dto.request.MapFilterRequest;
import bjs.zangbu.map.dto.request.MapListRequest;
import bjs.zangbu.map.dto.request.MapSearchRequest;
import bjs.zangbu.map.dto.response.AptDetailResponse;
import bjs.zangbu.map.dto.response.MapCategoryResponse;
import bjs.zangbu.map.dto.response.MapListResponse;
import bjs.zangbu.map.dto.response.MapSearchResponse;
import bjs.zangbu.map.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@Log4j2
public class MapController {
    private final MapService mapService;

    // 필터링된 매물 목록 조회
    @PostMapping
    public ResponseEntity<?> locate(@RequestBody MapFilterRequest req) {
        try {
            List<MapListResponse> result = mapService.locateWithFilter(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("필터링된 매물 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("서버에서 필터링된 매물을 불러오는데 실패했습니다.");
        }
    }

    // 전체 매물 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> list() {
        try {
            List<MapListResponse> result = mapService.getAllBuildingLocations();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("지도 매물 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("서버에서 매물을 불러오는데 실패했습니다.");
        }
    }

    // 키워드 검색
    @GetMapping
    public ResponseEntity<?> search(@ModelAttribute MapSearchRequest req) {
        try {
            List<MapSearchResponse> result = mapService.search(req);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생", e);
            return ResponseEntity.status(500).body("장소 검색 중 오류가 발생했습니다.");
        }
    }

    // 카테고리 검색
    @GetMapping("/category")
    public ResponseEntity<?> category(@ModelAttribute MapCategoryRequest req) {
        try {
            List<MapCategoryResponse> result = mapService.category(req);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("카테고리 검색 중 오류 발생", e);
            return ResponseEntity.status(500).body("카테고리 검색 중 오류가 발생했습니다.");
        }
    }

    // 아파트 상세 정보 조회 (매매 종류, 면적, 상세 주소)
    @GetMapping("/apt/{buildingId}")
    public ResponseEntity<?> getAptDetail(@PathVariable Long buildingId) {
        try {
            AptDetailResponse result = mapService.getAptDetail(buildingId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("아파트 상세 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body("아파트 상세 정보를 불러오는데 실패했습니다.");
        }
    }
}
