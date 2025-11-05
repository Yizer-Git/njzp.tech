package com.example.Kcsj.service;

import com.example.Kcsj.dto.DiseaseEncyclopediaResponse;
import com.example.Kcsj.entity.DiseaseInfo;

import java.util.List;

/**
 * Service contract for disease encyclopedia aggregation and search.
 */
public interface DiseaseEncyclopediaService {

    /**
     * Retrieve aggregated encyclopedia data grouped by crop.
     *
     * @return response payload containing overview stats and grouped diseases
     */
    DiseaseEncyclopediaResponse getEncyclopediaData();

    /**
     * Search diseases by keyword across code, name and crop.
     *
     * @param keyword search keyword, blank returns all diseases
     * @return matching disease list
     */
    List<DiseaseInfo> search(String keyword);
}

