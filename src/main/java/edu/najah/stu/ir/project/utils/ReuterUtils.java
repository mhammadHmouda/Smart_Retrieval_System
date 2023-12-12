package edu.najah.stu.ir.project.utils;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import edu.najah.stu.ir.project.models.ReuterDocument;
import java.util.List;
import java.util.stream.Collectors;

public class ReuterUtils {
    public static List<ReuterDocument> mapToReuters(SearchResponse<ReuterDocument> response) {
        return response.hits().hits()
                .stream().map(Hit::source)
                .collect(Collectors.toList());
    }
}
