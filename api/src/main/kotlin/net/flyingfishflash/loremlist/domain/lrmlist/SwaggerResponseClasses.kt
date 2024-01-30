package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.response.structure.Response
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import org.springframework.http.ProblemDetail

class ResponseProblemDetail : Response<ProblemDetail>(ProblemDetail.forStatus(404), "No Message", "No Method")

class ResponseLrmList : Response<LrmList>(LrmList(id = 0, name = "No Name"), "No Message", "No Method")

class ResponseListOfLrmList : Response<List<LrmList>>(content = listOf(LrmList(id = 0, name = "No Name")), "No Message", "No Method")
