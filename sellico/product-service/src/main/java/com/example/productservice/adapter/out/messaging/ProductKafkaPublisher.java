package com.example.productservice.adapter.out.messaging;

import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaPublisher implements ProductEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.product-events:product-events}")
    private String productEventsTopic;

    @Override
    public void publishProductCreated(Product product) {
        send(buildEvent(product, ProductEventMessage.EventType.PRODUCT_CREATED));
    }

    @Override
    public void publishProductUpdated(Product product) {
        send(buildEvent(product, ProductEventMessage.EventType.PRODUCT_UPDATED));
    }

    @Override
    public void publishProductDeleted(String productId) {
        ProductEventMessage event = ProductEventMessage.builder()
                .eventType(ProductEventMessage.EventType.PRODUCT_DELETED)
                .productId(productId)
                .isDeleted(true)
                .build();
        send(event);
    }

    @Override
    public void publishProductStatusChanged(Product product) {
        ProductEventMessage.EventType type = "ACTIVE".equalsIgnoreCase(product.getStatus())
                ? ProductEventMessage.EventType.PRODUCT_ACTIVATED
                : ProductEventMessage.EventType.PRODUCT_DEACTIVATED;
        send(buildEvent(product, type));
    }

    private ProductEventMessage buildEvent(Product product, ProductEventMessage.EventType type) {
        // Extract thumbnail url (primary image or first image)
        String thumbnail = Optional.ofNullable(product.getImages())
                .flatMap(imgs -> imgs.stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .findFirst()
                        .or(() -> imgs.stream().min(Comparator.comparingInt(i ->
                                i.getSortOrder() != null ? i.getSortOrder() : Integer.MAX_VALUE)))
                )
                .map(ProductImage::getUrl)
                .orElse(null);

        // Extract option names
        List<String> optionNames = Optional.ofNullable(product.getOptions())
                .map(opts -> opts.stream()
                        .map(o -> o.getName())
                        .collect(Collectors.toList()))
                .orElse(List.of());

        return ProductEventMessage.builder()
                .eventType(type)
                .productId(product.getId().toString())
                .sellerId(product.getSellerId() != null ? product.getSellerId().toString() : null)
                .categoryId(product.getCategoryId() != null ? product.getCategoryId().toString() : null)
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .status(product.getStatus())
                .minPrice(product.getMinPrice())
                .maxPrice(product.getMaxPrice())
                .ratingAvg(product.getRatingAvg())
                .ratingCount(product.getRatingCount())
                .soldCount(product.getSoldCount())
                .thumbnailUrl(thumbnail)
                .specifications(product.getSpecifications())
                .optionNames(optionNames)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .isDeleted(product.getIsDeleted())
                .build();
    }

    private void send(ProductEventMessage event) {
        String key = event.getProductId();
        kafkaTemplate.send(productEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published product event: type={}, productId={}, offset={}",
                                event.getEventType(), event.getProductId(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish product event: type={}, productId={}, error={}",
                                event.getEventType(), event.getProductId(), ex.getMessage(), ex);
                    }
                });
    }
}
