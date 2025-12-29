package com.deskit.deskit.tag.repository;

import com.deskit.deskit.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
