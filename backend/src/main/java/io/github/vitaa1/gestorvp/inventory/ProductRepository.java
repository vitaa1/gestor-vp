package io.github.vitaa1.gestorvp.inventory;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findByNormalizedName(String normalizedName);

	Optional<Product> findFirstBySearchNameOrderById(String searchName);

	Optional<Product> findByBarcode(String barcode);

	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into products (name, normalized_name, barcode, category, created_at)
			values (:name, :normalizedName, :barcode, :category, :createdAt)
			on conflict (normalized_name) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(@Param("name") String name, @Param("normalizedName") String normalizedName,
			@Param("barcode") String barcode, @Param("category") String category,
			@Param("createdAt") Instant createdAt);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			update products
			set barcode = :barcode
			where id = :productId
			  and (barcode is null or barcode = :barcode)
			""", nativeQuery = true)
	int claimBarcode(@Param("productId") long productId, @Param("barcode") String barcode);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			update products
			set category = :category
			where id = :productId
			  and (category is null or category = :category)
			""", nativeQuery = true)
	int claimCategory(@Param("productId") long productId, @Param("category") String category);

}
