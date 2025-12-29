package com.deskit.deskit.account.entity;

import com.deskit.deskit.account.enums.SellerGradeEnum;
import com.deskit.deskit.account.enums.SellerGradeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seller_grade")
public class SellerGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "grade")
    private SellerGradeEnum grade;

    @Column(name = "grade_status")
    private SellerGradeStatus gradeStatus;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "company_id")
    private Long companyId;
}
