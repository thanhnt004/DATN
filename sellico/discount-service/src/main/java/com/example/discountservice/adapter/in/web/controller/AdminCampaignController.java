package com.example.discountservice.adapter.in.web.controller;

import com.example.discountservice.adapter.in.web.request.CreateCampaignRequest;
import com.example.discountservice.adapter.in.web.request.UpdateCampaignRequest;
import com.example.discountservice.adapter.in.web.response.CampaignResponse;
import com.example.discountservice.application.port.in.CampaignUseCase;
import com.example.discountservice.domain.model.Campaign;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.net.URI;
import java.util.UUID;

/**
 * Admin campaign management
 * Base path: /api/v1/admin/campaigns
 */
@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {

    private final CampaignUseCase campaignUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> create(@Valid @RequestBody CreateCampaignRequest req) {
        Campaign campaign = campaignUseCase.createCampaign(
                new CampaignUseCase.CreateCampaignCommand(
                        req.getSellerId(), req.getName(), req.getDescription(),
                        req.getCampaignType(), req.getStartDate(), req.getEndDate()));
        CampaignResponse resp = CampaignResponse.from(campaign);
        return ResponseEntity.created(URI.create("/api/v1/admin/campaigns/" + resp.getId()))
                .body(ApiResponse.success(resp));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CampaignResponse>>> list(
            @RequestParam(value = "sellerId", required = false) UUID sellerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<CampaignResponse> resp = campaignUseCase.listCampaigns(sellerId, status, page, size)
                .map(CampaignResponse::from);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.from(campaignUseCase.getCampaign(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> update(@PathVariable("id") UUID id,
                                                                  @RequestBody UpdateCampaignRequest req) {
        Campaign updated = campaignUseCase.updateCampaign(id,
                new CampaignUseCase.UpdateCampaignCommand(
                        req.getName(), req.getDescription(), req.getStatus(),
                        req.getStartDate(), req.getEndDate()));
        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        campaignUseCase.deleteCampaign(id, null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

