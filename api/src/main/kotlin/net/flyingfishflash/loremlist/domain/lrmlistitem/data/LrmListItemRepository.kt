package net.flyingfishflash.loremlist.domain.lrmlistitem.data

import org.springframework.data.jpa.repository.JpaRepository

interface LrmListItemRepository : JpaRepository<LrmListItem, Long>
