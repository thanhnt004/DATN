package com.example.categoryservice.repository;

import com.example.categoryservice.dto.projection.CategoryBasicProjection;
import com.example.categoryservice.dto.projection.CategoryProjection;
import com.example.categoryservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("SELECT c.id as id, c.name as name FROM Category c WHERE c.id IN :ids")
    List<CategoryBasicProjection> findBasicProjectionsByIds(@Param("ids") List<UUID> ids);

    @Query("""
    SELECT c.id as id, c.parent.id as parentId, c.name as name, 
           c.slug as slug, c.path as path, c.level as level, 
           c.sortOrder as sortOrder, c.imageUrl as imageUrl, c.iconUrl as iconUrl,
           c.status as status, c.description as description 
    FROM Category c 
    WHERE c.level <= :depth
    ORDER BY c.level, c.sortOrder
""")
    List<CategoryProjection> findAllByLevelLessThanEqual(@Param("depth") int depth);

    @Query("""
    SELECT c.id as id, c.parent.id as parentId, c.name as name, 
           c.slug as slug, c.path as path, c.level as level, 
           c.sortOrder as sortOrder, c.imageUrl as imageUrl, c.iconUrl as iconUrl,
           c.status as status, c.description as description 
    FROM Category c 
    WHERE c.path LIKE CONCAT(:parentPath, '/%') 
      AND c.level <= :maxLevel
    ORDER BY c.level, c.sortOrder
""")
    List<CategoryProjection> findChildrenByPath(String parentPath, int maxLevel);

    /**
     * Find all descendant category IDs (including self) by materialized path prefix.
     */
    @Query("SELECT c.id FROM Category c WHERE c.path LIKE CONCAT(:pathPrefix, '%')")
    List<UUID> findDescendantIds(@Param("pathPrefix") String pathPrefix);

    boolean existsByParentIdAndSlug(UUID parentId, String slug);

    boolean existsByParentId(UUID parentId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE categories " +
            "SET path = REPLACE(path, :oldPath, :newPath), " +
            "    level = level - :oldLevel + :newLevel " +
            "WHERE path LIKE CONCAT(:oldPath, '%')",
            nativeQuery = true)
    int updateChildPaths(@Param("oldPath") String oldPath,
                         @Param("newPath") String newPath,
                         @Param("oldLevel") int oldLevel,
                         @Param("newLevel") int newLevel);
    @Modifying
    @Query("UPDATE Category c SET c.sortOrder = c.sortOrder + :increment " +
            "WHERE (c.parent.id = :parentId OR (:parentId IS NULL AND c.parent IS NULL)) " +
            "AND c.sortOrder >= :fromOrder")
    void shiftSortOrder(@Param("parentId") UUID parentId,
                        @Param("fromOrder") Integer fromOrder,
                        @Param("increment") int increment);
    @Query(
            "SELECT COALESCE(MAX(C.sortOrder), 0) + 1 FROM Category C " +
            "WHERE (C.parent.id = :parentId OR (:parentId IS NULL AND C.parent IS NULL))"
    )
    Integer findNextSortOrder(@Param("parentId") UUID parentId);
    @Modifying
    @Query("UPDATE Category c " +
            "SET c.sortOrder = c.sortOrder + :increment " +
            "WHERE (c.parent.id = :parentId OR (:parentId IS NULL AND c.parent IS NULL)) " +
            "AND c.sortOrder BETWEEN :start AND :end")
    void reorderRange(@Param("parentId") UUID parentId,
                      @Param("start") Integer start,
                      @Param("end") Integer end,
                      @Param("increment") int increment);
    @Modifying
    @Query(value = "UPDATE categories " +
            "SET breadcrumb = CAST(REPLACE(CAST(breadcrumb AS TEXT), :oldBreadcrumbPrefix, :newBreadcrumbPrefix) AS JSONB) " +
            "WHERE path LIKE CONCAT(:oldPath, '%')", nativeQuery = true)
    void updateChildBreadcrumbs(@Param("oldPath") String oldPath,
                                @Param("oldBreadcrumbPrefix") String oldBreadcrumbPrefix,
                                @Param("newBreadcrumbPrefix") String newBreadcrumbPrefix);
}
