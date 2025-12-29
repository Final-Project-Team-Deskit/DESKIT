package com.deskit.deskit.account.entity;

import com.deskit.deskit.account.enums.JobCategory;
import com.deskit.deskit.account.enums.MBTI;
import com.deskit.deskit.account.enums.MemberStatus;
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
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "login_id", nullable = false)
    private String loginId;

    @Column(name = "profile")
    private String profile;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "is_agreed", nullable = false)
    private boolean isAgreed;

    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "mbti")
    private MBTI mbti;

    @Column(name = "job_category")
    private JobCategory jobCategory;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
