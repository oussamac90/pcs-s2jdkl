package com.pcs.vcms.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.Version;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.locationtech.jts.geom.Point;

import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;

/**
 * Entity class representing a berth in the port management system.
 * Implements comprehensive berth management with support for automated allocation,
 * conflict resolution, and schedule optimization.
 */
@Entity
@Table(name = "berths")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Index(name = "idx_berth_status", columnList = "status")
public class Berth {

    /**
     * Enum representing possible states of a berth with additional metadata
     */
    public enum BerthStatus {
        AVAILABLE("Available", "Berth is available for allocation"),
        OCCUPIED("Occupied", "Berth is currently occupied by a vessel"),
        UNDER_MAINTENANCE("Under Maintenance", "Berth is unavailable due to maintenance");

        private final String displayName;
        private final String description;

        BerthStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 2, max = 50)
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotNull
    @Min(1)
    @Column(name = "length", nullable = false)
    private Double length;

    @NotNull
    @Min(1)
    @Column(name = "depth", nullable = false)
    private Double depth;

    @Column(name = "max_vessel_size")
    private String maxVesselSize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BerthStatus status;

    @Column(name = "location", columnDefinition = "geometry")
    private Point location;

    @Version
    @Column(name = "version")
    private Integer version;

    @OneToMany(mappedBy = "berth")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<BerthAllocation> allocations = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback executed before persisting the entity
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BerthStatus.AVAILABLE;
        }
        if (version == null) {
            version = 0;
        }
        if (allocations == null) {
            allocations = new HashSet<>();
        }
    }

    /**
     * JPA lifecycle callback executed before updating the entity
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}