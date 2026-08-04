package net.flyingfishflash.loremlist.persistence;

import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;

public record SuccinctLrmComponentPair(LrmListSuccinct list, LrmItemSuccinct item) {}
