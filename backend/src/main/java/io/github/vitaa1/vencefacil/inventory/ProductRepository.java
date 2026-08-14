package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findByNormalizedName(String normalizedName);

	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into products (name, normalized_name, created_at)
			values (:name, :normalizedName, :createdAt)
			on conflict (normalized_name) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(@Param("name") String name, @Param("normalizedName") String normalizedName,
			@Param("createdAt") Instant createdAt);

}
