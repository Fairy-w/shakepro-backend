package com.shakepro.repository;

import com.shakepro.entity.CrawlImportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrawlImportRecordRepository extends JpaRepository<CrawlImportRecord, Long> {

    Optional<CrawlImportRecord> findByDetailUrl(String detailUrl);

    @Query("""
            SELECT r.detailUrl
            FROM CrawlImportRecord r
            WHERE r.status = 'SUCCESS'
              AND r.savedCocktailId IS NOT NULL
              AND r.detailUrl IN :detailUrls
            """)
    List<String> findSuccessfulDetailUrlsIn(@Param("detailUrls") Collection<String> detailUrls);

    @Query("""
            SELECT r.detailUrl
            FROM CrawlImportRecord r
            WHERE r.detailUrl IN :detailUrls
              AND (
                    (r.status = 'SUCCESS' AND r.savedCocktailId IS NOT NULL)
                    OR r.status = 'IGNORED_NON_DETAIL'
                  )
            """)
    List<String> findAlreadyHandledDetailUrlsIn(@Param("detailUrls") Collection<String> detailUrls);
}
