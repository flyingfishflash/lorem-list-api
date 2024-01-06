package net.flyingfishflash.loremlist.domain.lrmlist.data

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface LrmListRepository : JpaRepository<LrmList, Long>
