package net.flyingfishflash.loremlist.core.response.structure;

import java.util.UUID;

/** Describes the structure of API responses sent to a client */
public sealed interface Response<T> permits ResponseProblem, ResponseSuccess {
  UUID id();

  Disposition disposition();

  String method();

  String instance();

  String message();

  int size();

  T content();
}
