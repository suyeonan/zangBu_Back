package bjs.zangbu.map.service;

import bjs.zangbu.map.dto.request.MapCategoryRequest;
import bjs.zangbu.map.dto.request.MapFilterRequest;
import bjs.zangbu.map.dto.request.MapListRequest;
import bjs.zangbu.map.dto.request.MapSearchRequest;
import bjs.zangbu.map.dto.response.AptDetailResponse;
import bjs.zangbu.map.dto.response.MapCategoryResponse;
import bjs.zangbu.map.dto.response.MapListResponse;
import bjs.zangbu.map.dto.response.MapSearchResponse;

import java.util.List;

/**
 * MapService 인터페이스
 * - 외부 API 연동을 통해 주소 리스트를 위도/경도로 변환하는 기능을 정의
 */

public interface MapService {

    List<MapListResponse> locateWithFilter(MapFilterRequest req);

    List<MapListResponse> getAllBuildingLocations();

    List<MapSearchResponse> search(MapSearchRequest req);

    List<MapCategoryResponse> category(MapCategoryRequest req);

    AptDetailResponse getAptDetail(Long buildingId);
}
