package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "address_line1", nullable = false)
    String addressLine1;

    @Column(name = "address_line2")
    String addressLine2;

    @Column(length = 100)
    String city;

    @Column(length = 100)
    String state;

    @Column(length = 100)
    String country;

    @Column(name = "postal_code", length = 20)
    String postalCode;

    @Builder.Default
    @Column(name = "is_default")
    Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}