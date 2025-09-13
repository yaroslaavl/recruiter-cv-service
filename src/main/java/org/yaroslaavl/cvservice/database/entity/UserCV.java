package org.yaroslaavl.cvservice.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cv", schema = "cv_data")
public class UserCV {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @ColumnDefault(value = "false")
    @Column(name = "is_main", nullable = false)
    private Boolean isMain;

    @Size(max = 100)
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void setCreationDateTime() {
        this.uploadedAt = LocalDateTime.now();
    }
}
