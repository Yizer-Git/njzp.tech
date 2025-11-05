package com.example.Kcsj.service;

import com.example.Kcsj.dto.DiseaseMappingResult;

/**
 * Disease label mapping service for converting YOLO detection labels
 * to knowledge base disease entities.
 */
public interface DiseaseMapperService {

    /**
     * Map a YOLO detection label to disease information from knowledge base.
     *
     * @param label    detected label from YOLO model, e.g., "common_rust（普通锈病）"
     * @param cropType crop type identifier, e.g., "corn", "rice", "tomato", "strawberry"
     * @return disease mapping result with disease ID and details
     * @throws IllegalArgumentException if label cannot be mapped
     */
    DiseaseMappingResult mapLabelToDisease(String label, String cropType);

    /**
     * Check if a given label represents a healthy/normal state.
     *
     * @param label detected label
     * @return true if label indicates healthy state
     */
    boolean isHealthyLabel(String label);
    
    /**
     * Map a Flask label to disease ID.
     * Simplified version that returns only the disease ID.
     *
     * @param label    detected label from Flask model
     * @param cropType crop type identifier
     * @return disease ID, or null if mapping fails
     */
    Long mapFlaskLabelToDiseaseId(String label, String cropType);
}

