package com.shop.product.metadata.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.product.metadata.dto.TcgPSetInfoDto;
import com.shop.product.metadata.entity.TcgPSetName;

public interface TcgPSetNameRepository extends JpaRepository<TcgPSetName, Long> {

  interface SetNameTypeIdProjection {
    Long getProductLineId();

    Long getSetNameId();

    Long getProductTypeId();
  }

  List<TcgPSetName> findAllByCategoryId(Long categoryId, Sort sort);

  Optional<TcgPSetName> findBySetNameId(Long setNameId);

  @Query(value = """
      SELECT pt.product_line_id AS productLineId, sn.set_name_id AS setNameId, pt.product_type_id AS productTypeId
      FROM set_names sn
      JOIN tcg_p_product_types pt ON sn.category_id = pt.product_line_id
      WHERE pt.product_line_id in :productLineIds AND (pt.product_name = 'Cards' OR pt.product_name = 'Sealed Products')
      ORDER BY sn.release_date DESC
      """, nativeQuery = true)
  List<SetNameTypeIdProjection> findSetNameAndProductTypeIdsByProductLineIdsOrderByReleaseDateDesc(
      @Param("productLineIds") List<Long> productLineIds);

      @Query(value = """
        SELECT pt.product_line_id AS productLineId, sn.set_name_id AS setNameId, pt.product_type_id AS productTypeId
        FROM set_names sn
        JOIN tcg_p_product_types pt ON sn.category_id = pt.product_line_id
        WHERE pt.product_line_id = 1 AND pt.product_name = 'Sealed Products'
        ORDER BY sn.release_date DESC
        """, nativeQuery = true)
    List<SetNameTypeIdProjection> findSetNameAndProductTypeIdsByCategoryIdForMagicOrderByReleaseDateDesc(@Param("categoryId") Long categoryId);



  @Query("""
      SELECT new com.shop.product.metadata.dto.TcgPSetInfoDto(sn.abbreviation, sn.name, sn.urlName, sn.releaseDate)
      FROM TcgPSetName sn
      WHERE sn.categoryId = :productLineId
      ORDER BY sn.releaseDate DESC
      """)
  List<TcgPSetInfoDto> findSetInfoListByProductLineIdOrderByReleaseDateDesc(@Param("productLineId") Long productLineId);

  @Query("""
      SELECT new com.shop.product.metadata.dto.TcgPSetInfoDto(sn.abbreviation, sn.name, sn.urlName, sn.releaseDate)
      FROM TcgPSetName sn
      WHERE sn.categoryId = :productLineId
        AND sn.releaseDate < :releaseDateCutoff
      ORDER BY sn.releaseDate DESC
      """)
  List<TcgPSetInfoDto> findSetInfoListByProductLineIdAndReleaseDateBeforeOrderByReleaseDateDesc(
      @Param("productLineId") Long productLineId,
      @Param("releaseDateCutoff") LocalDateTime releaseDateCutoff);

  @Query("""
      SELECT new com.shop.product.metadata.dto.TcgPSetInfoDto(sn.abbreviation, sn.name, sn.urlName, sn.releaseDate)
      FROM TcgPSetName sn
      WHERE (sn.urlName = :setName OR sn.abbreviation = :setName)
        AND sn.categoryId IN (
          SELECT pl.productLineId
          FROM TcgPProductLine pl
          WHERE pl.productLineName = :game
        )
      """)
  Optional<TcgPSetInfoDto> findSetInfoByGameAndSetName(@Param("game") String game, @Param("setName") String setName);

  @Query("""
      SELECT new com.shop.product.metadata.dto.TcgPSetInfoDto(sn.abbreviation, sn.name, sn.urlName, sn.releaseDate)
      FROM TcgPSetName sn
      WHERE sn.abbreviation = :setName
        AND sn.categoryId = :categoryId
      """)
  Optional<TcgPSetInfoDto> findSetInfoByCategoryIdAndSetName(@Param("categoryId") Long categoryId, @Param("setName") String setName);
}
