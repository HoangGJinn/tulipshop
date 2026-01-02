package com.tulip.entity;

import com.tulip.entity.product.Product;
import com.tulip.entity.product.ProductStock;
import com.tulip.entity.product.ProductVariant;
import com.tulip.entity.product.Size;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    // Lưu các FK để tham chiếu lịch sử (có thể null nếu sản phẩm bị xóa sau này)
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private ProductStock stock;

    private String sku; // Lưu cứng SKU tại thời điểm mua

    private Integer quantity;

    @Column(name = "price_at_purchase")
    private BigDecimal priceAtPurchase; // Quan trọng: Giá tại thời điểm mua

    // Snapshot fields - Đóng băng thông tin sản phẩm tại thời điểm đặt hàng
    @Column(name = "snap_product_name")
    private String snapProductName;

    @Column(name = "snap_price")
    private BigDecimal snapPrice;

    @Column(name = "snap_thumbnail_url")
    private String snapThumbnailUrl;
}