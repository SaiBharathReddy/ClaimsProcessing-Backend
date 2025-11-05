package com.beagle.claims.repository;

import com.beagle.claims.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy,String> {

}
