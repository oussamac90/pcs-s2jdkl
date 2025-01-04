package com.pcs.vcms.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Index;
import javax.persistence.Convert;
import javax.persistence.EntityListeners;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity class representing a pre-arrival notification with enhanced security and validation.
 * Implements data classification and encryption for sensitive information.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Entity
@Table(name = "pre_arrival_notifications", indexes = {
    @Index(name = "idx_vessel_call_id", columnList = "vessel_call_id"),
    @Index(name = "idx_submitted_at", columnList = "submitted_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreArrivalNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vessel_call_id", nullable = false)
    @NotNull(message = "Vessel call reference is required")
    private VesselCall vesselCall;

    @Column(name = "submitted_by", nullable = false, length = 100)
    @NotNull(message = "Submitter information is required")
    @Size(min = 3, max = 100, message = "Submitter name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s.-]+$", message = "Submitter name contains invalid characters")
    private String submittedBy;

    @Column(name = "cargo_details", columnDefinition = "TEXT")
    @ColumnTransformer(
        read = "pgp_sym_decrypt(cargo_details::bytea, current_setting('app.encryption_key'))",
        write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))"
    )
    private String cargoDetails;

    @Column(name = "crew_list", columnDefinition = "TEXT")
    @ColumnTransformer(
        read = "pgp_sym_decrypt(crew_list::bytea, current_setting('app.encryption_key'))",
        write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))"
    )
    private String crewList;

    @Column(name = "submitted_at", nullable = false)
    @NotNull(message = "Submission timestamp is required")
    private LocalDateTime submittedAt;

    @Column(name = "security_classification", nullable = false, length = 20)
    @NotNull(message = "Security classification is required")
    @Pattern(regexp = "^(PUBLIC|CONFIDENTIAL|RESTRICTED)$", 
            message = "Invalid security classification")
    private String securityClassification;

    @Column(name = "encryption_version", nullable = false, length = 10)
    private String encryptionVersion;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived;

    /**
     * Initializes default values and performs validation before persisting.
     */
    @PrePersist
    protected void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        
        if (securityClassification == null) {
            securityClassification = "CONFIDENTIAL";
        }
        
        if (encryptionVersion == null) {
            encryptionVersion = "AES256-v1";
        }
        
        if (isArchived == null) {
            isArchived = false;
        }

        validateSensitiveData();
    }

    /**
     * Validates sensitive data fields based on security classification.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    private void validateSensitiveData() {
        if (cargoDetails != null && cargoDetails.length() > 10000) {
            throw new IllegalArgumentException("Cargo details exceed maximum allowed length");
        }
        
        if (crewList != null && crewList.length() > 5000) {
            throw new IllegalArgumentException("Crew list exceeds maximum allowed length");
        }

        if ("RESTRICTED".equals(securityClassification)) {
            if (cargoDetails == null || crewList == null) {
                throw new IllegalArgumentException(
                    "Cargo details and crew list are mandatory for RESTRICTED classification"
                );
            }
        }
    }

    /**
     * Custom builder implementation to ensure validation of sensitive data
     * and proper initialization of security fields.
     */
    public static class PreArrivalNotificationBuilder {
        public PreArrivalNotification build() {
            if (this.submittedAt == null) {
                this.submittedAt = LocalDateTime.now();
            }
            
            if (this.securityClassification == null) {
                this.securityClassification = "CONFIDENTIAL";
            }
            
            if (this.encryptionVersion == null) {
                this.encryptionVersion = "AES256-v1";
            }
            
            if (this.isArchived == null) {
                this.isArchived = false;
            }

            PreArrivalNotification notification = new PreArrivalNotification();
            notification.setId(this.id);
            notification.setVesselCall(this.vesselCall);
            notification.setSubmittedBy(this.submittedBy);
            notification.setCargoDetails(this.cargoDetails);
            notification.setCrewList(this.crewList);
            notification.setSubmittedAt(this.submittedAt);
            notification.setSecurityClassification(this.securityClassification);
            notification.setEncryptionVersion(this.encryptionVersion);
            notification.setIsArchived(this.isArchived);
            
            notification.validateSensitiveData();
            return notification;
        }
    }
}