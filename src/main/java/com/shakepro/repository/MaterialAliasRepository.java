package com.shakepro.repository;

import com.shakepro.entity.MaterialAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MaterialAliasRepository extends JpaRepository<MaterialAlias, Long> {

    List<MaterialAlias> findByAliasNormalizedInOrderByPriorityAscIdAsc(Collection<String> aliasNormalized);
}
