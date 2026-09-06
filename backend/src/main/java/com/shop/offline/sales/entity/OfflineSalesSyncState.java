package com.shop.offline.sales.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "offline_sales_sync_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesSyncState {

    @Builder.Default
    @Id
    private Long id = 1L; // 고정값 1L

    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastSuccessAt;
    
    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

}
