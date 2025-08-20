package bjs.zangbu.map.service;

import bjs.zangbu.building.mapper.BuildingMapper;
import bjs.zangbu.building.vo.Building;
import bjs.zangbu.complexList.mapper.ComplexListMapper;
import bjs.zangbu.complexList.vo.ComplexList;
import bjs.zangbu.map.dto.request.MapCategoryRequest;
import bjs.zangbu.map.dto.request.MapFilterRequest;
import bjs.zangbu.map.dto.request.MapListRequest;
import bjs.zangbu.map.dto.request.MapSearchRequest;
import bjs.zangbu.map.dto.response.AptDetailResponse;
import bjs.zangbu.map.dto.response.MapCategoryResponse;
import bjs.zangbu.map.dto.response.MapListResponse;
import bjs.zangbu.map.dto.response.MapSearchResponse;
import bjs.zangbu.map.mapper.MapLocationMapper;
import bjs.zangbu.map.util.KakaoMapClient;
import bjs.zangbu.map.vo.MapLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import bjs.zangbu.map.dto.BuildingComplexInfo;

/**
 * MapService 구현체
 * - CodefClient를 이용해 외부 지오코딩 API 호출 후 DTO 매핑 처리
 */

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {
    private final MapLocationMapper locationMapper;

    // 외부 API 호출용 클라이언트 (RestTemplate 래핑)
    private final KakaoMapClient kakaoClient;
    
    // Building과 ComplexList 정보를 조회하기 위한 Mapper
    private final BuildingMapper buildingMapper;
    private final ComplexListMapper complexListMapper;

    @Override
    public List<MapListResponse> getAllBuildingLocations() {
        List<BuildingComplexInfo> buildings = locationMapper.findAllBuildingComplexInfo();
        return buildings.stream()
                .map(building -> {
                    // 주소 필드들을 조합하여 완전한 주소 생성 (지번 주소 우선)
                    String fullAddress = Stream.of(
                            building.getSido(),
                            building.getSigungu(),
                            building.getAddress() // 지번 주소 (예: "도곡동 963")
                    )
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(" "));

                    return new MapListResponse(building.getBuildingId(), building.getBuildingName(), fullAddress);
                })
                .toList();
    }

    // MapSearch DTO 이용하여 입력받은 쿼리로 검색하는 메서드
    @Override
    public List<MapSearchResponse> search(MapSearchRequest req) {
        if (req.getQuery() == null || req.getQuery().isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        return kakaoClient.searchByKeyword((req.getQuery()));
    }

    // MapCategory DTO 활용
    @Override
    public List<MapCategoryResponse> category(MapCategoryRequest req) {
        // 유효성 검사
        if (req.getCategory_group_code() == null || req.getRadius() <= 0) {
            throw new IllegalArgumentException("파라미터가 잘못되었습니다.");
        }
        return kakaoClient.searchByCategory(req);
    }

    // /map 필터 기능
    @Override
    public List<MapListResponse> locateWithFilter(MapFilterRequest req) {
        List<BuildingComplexInfo> buildings = locationMapper.findLocationsByFilters(
                req.getSaleTypes(),
                req.getPropertyTypes(),
                req.getPriceMin(),
                req.getPriceMax());

        return buildings.stream()
                .map(building -> {
                    String fullAddress = Stream.of(
                            building.getSido(),
                            building.getSigungu(),
                            building.getAddress())
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(" "));
                    return new MapListResponse(building.getBuildingId(), building.getBuildingName(), fullAddress);
                })
                .toList();
    }

    // 아파트 상세 정보 조회 (매매 종류, 면적, 상세 주소)
    @Override
    public AptDetailResponse getAptDetail(Long buildingId) {
        // Building 정보 조회
        Building building = buildingMapper.getBuildingById(buildingId);
        if (building == null) {
            throw new IllegalArgumentException("해당 매물을 찾을 수 없습니다. buildingId: " + buildingId);
        }

        // ComplexList 정보 조회
        ComplexList complexList = complexListMapper.getComplexListByBuildingId(buildingId);
        if (complexList == null) {
            throw new IllegalArgumentException("해당 단지 정보를 찾을 수 없습니다. buildingId: " + buildingId);
        }

        return new AptDetailResponse(
                building.getSaleType().toString(),  // 매매 종류
                building.getSize(),                 // 면적
                complexList.getDong()              // 상세 주소 (동 정보)
        );
    }
}
