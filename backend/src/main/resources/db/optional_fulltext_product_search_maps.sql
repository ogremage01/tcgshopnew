-- 선택 적용: MariaDB/MySQL에서 product_name / product_name_ko 풀텍스트 인덱스.
-- 애플리케이션 QueryDSL은 현재 LIKE %% 패턴을 사용하므로, MATCH() 기반으로 바꾸기 전에는 효과가 없다.
-- DBA 검토 후 수동 실행.

-- ALTER TABLE product_search_maps
--   ADD FULLTEXT INDEX ft_product_search_names (product_name, product_name_ko);
