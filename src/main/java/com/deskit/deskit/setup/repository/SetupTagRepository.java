package com.deskit.deskit.setup.repository;

import com.deskit.deskit.setup.entity.SetupTag;
import com.deskit.deskit.setup.entity.SetupTag.SetupTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetupTagRepository extends JpaRepository<SetupTag, SetupTagId> {
}
