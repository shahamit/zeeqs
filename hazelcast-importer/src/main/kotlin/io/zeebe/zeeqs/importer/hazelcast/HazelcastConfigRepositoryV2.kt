package io.zeebe.zeeqs.importer.hazelcast

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface HazelcastConfigRepositoryV2 : CrudRepository<HazelcastConfigV2, String> {

}