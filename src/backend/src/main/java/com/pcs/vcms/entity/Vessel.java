package com.pcs.vcms.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.EntityListeners;
import javax.persistence.GenerationType;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * Entity class representing a vessel in the Port Community System.
 * Contains essential vessel information required for port operations and vessel call management.
 * Implements comprehensive validation and security measures.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Entity
@Table(name = "vessels", indexes = {
    @Index(name = "idx_vessel_imo", columnList = "imo_number", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vessel_seq")
    @SequenceGenerator(name = "vessel_seq", sequenceName = "vessel_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "imo_number", nullable = false, unique = true, length = 7)
    @Pattern(regexp = "^\\d{7}$", message = "IMO number must be exactly 7 digits")
    @NotNull(message = "IMO number is required")
    private String imoNumber;

    @Column(name = "name", nullable = false, length = 100)
    @Size(min = 2, max = 100, message = "Vessel name must be between 2 and 100 characters")
    @NotNull(message = "Vessel name is required")
    private String name;

    @Column(name = "type", length = 50)
    @Size(max = 50, message = "Vessel type must not exceed 50 characters")
    private String type;

    @Column(name = "flag", length = 3)
    @Size(min = 2, max = 3, message = "Flag must be 2 or 3 characters ISO country code")
    private String flag;

    @Column(name = "length", precision = 10, scale = 2)
    @Min(value = 0, message = "Length must be positive")
    private Float length;

    @Column(name = "width", precision = 10, scale = 2)
    @Min(value = 0, message = "Width must be positive")
    private Float width;

    @Column(name = "max_draft", precision = 10, scale = 2)
    @Min(value = 0, message = "Maximum draft must be positive")
    private Float maxDraft;

    @Column(name = "owner", length = 100)
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    private String owner;

    @OneToMany(mappedBy = "vessel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private Set<VesselCall> vesselCalls = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}