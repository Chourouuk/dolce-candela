package com.dolcecandela.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomizationOptionRepository extends JpaRepository<CustomizationOption, Long> {

    List<CustomizationOption> findAllByActiveTrueOrderByTypeAscIdAsc();

    List<CustomizationOption> findAllByTypeAndActiveTrueOrderByIdAsc(OptionType type);

    List<CustomizationOption> findAllByTypeAndCodeInAndActiveTrue(OptionType type, Collection<String> codes);

    Optional<CustomizationOption> findByTypeAndCodeAndActiveTrue(OptionType type, String code);

    boolean existsByTypeAndCode(OptionType type, String code);
}
