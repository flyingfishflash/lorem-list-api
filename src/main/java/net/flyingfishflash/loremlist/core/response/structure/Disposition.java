package net.flyingfishflash.loremlist.core.response.structure;

/**
 * Represents the disposition of any request from a client.
 *
 * <p>Every response to a client must include exactly one Disposition.
 */
public interface Disposition {
  String nameAsLowercase();
}
