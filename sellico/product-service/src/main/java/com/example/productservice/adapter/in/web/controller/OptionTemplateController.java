package com.example.productservice.adapter.in.web.controller;

import com.example.productservice.adapter.in.web.request.CreateOptionTemplateRequest;
import com.example.productservice.adapter.in.web.request.UpdateOptionTemplateRequest;
import com.example.productservice.adapter.in.web.response.OptionTemplateResponse;
import com.example.productservice.adapter.in.web.response.OptionTemplateResponse.OptionTemplateValueResponse;
import com.example.productservice.adapter.out.persistence.entity.ProductOptionEntity;
import com.example.productservice.adapter.out.persistence.entity.ProductOptionValueEntity;
import com.example.productservice.adapter.out.persistence.repository.ProductOptionJpaRepository;
import com.example.productservice.application.port.out.SellerClientPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@RestController
@RequiredArgsConstructor
public class OptionTemplateController {

    private final ProductOptionJpaRepository repo;
    private final SellerClientPort sellerClientPort;

    // ─────────────────────────────────────────────
    // ADMIN endpoints — global option templates
    // ─────────────────────────────────────────────

    @GetMapping("/api/v1/admin/option-templates")
    public ResponseEntity<ApiResponse<List<OptionTemplateResponse>>> adminList() {
        List<OptionTemplateResponse> list = repo.findTemplatesBySource("ADMIN")
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/api/v1/admin/option-templates")
    public ResponseEntity<ApiResponse<OptionTemplateResponse>> adminCreate(
            @Valid @RequestBody CreateOptionTemplateRequest req) {
        if (repo.existsTemplateByNameIgnoreCaseAndSellerIdIsNull(req.getName())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "400", "Option template '" + req.getName() + "' đã tồn tại", null));
        }
        ProductOptionEntity entity = buildTemplateEntity(null, "ADMIN", req.getName(), req.getValues());
        entity = repo.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(entity)));
    }

    @PutMapping("/api/v1/admin/option-templates/{id}")
    public ResponseEntity<ApiResponse<OptionTemplateResponse>> adminUpdate(
            @PathVariable UUID id, @Valid @RequestBody UpdateOptionTemplateRequest req) {
        return repo.findById(id)
                .filter(e -> e.getProduct() == null) // only templates
                .map(entity -> {
                    entity.setName(req.getName());
                    replaceValues(entity, req.getValues());
                    entity = repo.save(entity);
                    return ResponseEntity.ok(ApiResponse.success(toResponse(entity)));
                }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ApiResponse<>(false, "404", "Không tìm thấy option template", null)));
    }

    @DeleteMapping("/api/v1/admin/option-templates/{id}")
    public ResponseEntity<ApiResponse<Void>> adminDelete(@PathVariable UUID id) {
        return repo.findById(id)
                .filter(e -> e.getProduct() == null)
                .map(entity -> {
                    repo.delete(entity);
                    return ResponseEntity.ok(ApiResponse.<Void>success(null));
                }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ApiResponse<>(false, "404", "Không tìm thấy option template", null)));
    }

    // ─────────────────────────────────────────────
    // SELLER endpoints — seller's own templates
    // ─────────────────────────────────────────────

    @GetMapping("/api/v1/seller/option-templates")
    public ResponseEntity<ApiResponse<List<OptionTemplateResponse>>> sellerList(
            @AuthenticationPrincipal Jwt jwt) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        List<OptionTemplateResponse> list = repo.findTemplatesBySellerId(sellerId)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/api/v1/seller/option-templates")
    public ResponseEntity<ApiResponse<OptionTemplateResponse>> sellerCreate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOptionTemplateRequest req) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        if (repo.existsTemplateByNameIgnoreCaseAndSellerId(req.getName(), sellerId)) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "400", "Option '" + req.getName() + "' đã tồn tại", null));
        }
        ProductOptionEntity entity = buildTemplateEntity(sellerId, "SELLER", req.getName(), req.getValues());
        entity = repo.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(entity)));
    }

    @PutMapping("/api/v1/seller/option-templates/{id}")
    public ResponseEntity<ApiResponse<OptionTemplateResponse>> sellerUpdate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateOptionTemplateRequest req) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        return repo.findById(id)
                .filter(e -> e.getProduct() == null && sellerId.equals(e.getSellerId()))
                .map(entity -> {
                    entity.setName(req.getName());
                    replaceValues(entity, req.getValues());
                    entity = repo.save(entity);
                    return ResponseEntity.ok(ApiResponse.success(toResponse(entity)));
                }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ApiResponse<>(false, "404", "Không tìm thấy option template", null)));
    }

    @DeleteMapping("/api/v1/seller/option-templates/{id}")
    public ResponseEntity<ApiResponse<Void>> sellerDelete(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        return repo.findById(id)
                .filter(e -> e.getProduct() == null && sellerId.equals(e.getSellerId()))
                .map(entity -> {
                    repo.delete(entity);
                    return ResponseEntity.ok(ApiResponse.<Void>success(null));
                }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ApiResponse<>(false, "404", "Không tìm thấy option template", null)));
    }

    // ─────────────────────────────────────────────
    // PUBLIC endpoints — available templates
    // ─────────────────────────────────────────────

    @GetMapping("/api/v1/option-templates/available")
    public ResponseEntity<ApiResponse<List<OptionTemplateResponse>>> available(
            @AuthenticationPrincipal Jwt jwt) {
        UUID sellerId = sellerClientPort.getSellerIdByUserId(UUID.fromString(jwt.getSubject()));
        List<OptionTemplateResponse> list = repo.findAvailableTemplatesForSeller(sellerId)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/api/v1/option-templates/admin")
    public ResponseEntity<ApiResponse<List<OptionTemplateResponse>>> publicAdminTemplates() {
        List<OptionTemplateResponse> list = repo.findTemplatesBySource("ADMIN")
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    private ProductOptionEntity buildTemplateEntity(UUID sellerId, String source, String name, List<String> values) {
        ProductOptionEntity entity = ProductOptionEntity.builder()
                .sellerId(sellerId)
                .source(source)
                .name(name.trim())
                .product(null) // template — no product
                .build();
        if (values != null) {
            IntStream.range(0, values.size()).forEach(i -> {
                String v = values.get(i).trim();
                if (!v.isEmpty()) {
                    entity.addValue(ProductOptionValueEntity.builder()
                            .value(v)
                            .sortOrder(i)
                            .build());
                }
            });
        }
        return entity;
    }

    private void replaceValues(ProductOptionEntity entity, List<String> values) {
        entity.getValues().clear();
        if (values != null) {
            IntStream.range(0, values.size()).forEach(i -> {
                String v = values.get(i).trim();
                if (!v.isEmpty()) {
                    entity.addValue(ProductOptionValueEntity.builder()
                            .value(v)
                            .sortOrder(i)
                            .build());
                }
            });
        }
    }

    private OptionTemplateResponse toResponse(ProductOptionEntity entity) {
        return OptionTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .source(entity.getSource())
                .sellerId(entity.getSellerId())
                .createdAt(entity.getCreatedAt())
                .values(entity.getValues().stream()
                        .map(v -> OptionTemplateValueResponse.builder()
                                .id(v.getId())
                                .value(v.getValue())
                                .sortOrder(v.getSortOrder())
                                .build())
                        .toList())
                .build();
    }
}
