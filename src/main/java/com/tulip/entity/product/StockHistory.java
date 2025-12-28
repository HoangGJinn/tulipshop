package com.tulip.entity.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private ProductStock stock;
    
    @Column(name = "previous_quantity")
    private Integer previousQuantity;
    
    @Column(name = "new_quantity")
    private Integer newQuantity;
    
    @Column(name = "change_amount")
    private Integer changeAmount;
    
    @Column(name = "admin_username")
    private String adminUsername;
    
    @Column(name = "reason")
    private String reason;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
