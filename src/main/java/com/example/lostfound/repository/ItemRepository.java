package com.example.lostfound.repository;

import com.example.lostfound.entity.Item;
import com.example.lostfound.entity.ItemStatus;
import com.example.lostfound.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByType(ItemType type);

    List<Item> findByReportedById(Long userId);

    List<Item> findByTypeAndStatus(ItemType type, ItemStatus status);

    List<Item> findByStatus(ItemStatus status);

    @Query("SELECT i FROM Item i WHERE " +
           "(:type IS NULL OR i.type = :type) AND " +
           "(:categoryId IS NULL OR i.category.id = :categoryId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:location IS NULL OR LOWER(i.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:startDate IS NULL OR i.dateLostOrFound >= :startDate) AND " +
           "(:endDate IS NULL OR i.dateLostOrFound <= :endDate) AND " +
           "(:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.color) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Item> searchItems(
            @Param("type") ItemType type,
            @Param("categoryId") Long categoryId,
            @Param("status") ItemStatus status,
            @Param("location") String location,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword
    );

    long countByType(ItemType type);
    long countByStatus(ItemStatus status);
}
