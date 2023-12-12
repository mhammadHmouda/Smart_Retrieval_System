package edu.najah.stu.ir.project.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import edu.najah.stu.ir.project.models.ReuterDocument;
import edu.najah.stu.ir.project.utils.ReuterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ESService {

    @Value("${es.index.name}")
    private String indexName;

    private final ElasticsearchClient es;

    public String deleteIndex() {
        try {
            es.indices().delete(c -> c.index(indexName));
            return "Index deleted successfully!";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public String addDocumentToIndex(ReuterDocument document) {
        try {
            var response = es.index(c -> c.index(indexName).document(document));
            return response.id();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public List<ReuterDocument> findAll() {
        try {
            var response = es.search(s -> s.index(indexName)
                    .query(q -> q.matchAll(QueryBuilders.matchAll().build()))
                    .size(10), ReuterDocument.class);

            return ReuterUtils.mapToReuters(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return Collections.emptyList();
        }
    }

    public String deleteById(String id) {
        try {
            var response = es.delete(c -> c.index(indexName).id(id));
            return response.id();
        } catch (Exception e){
            System.out.println(e.getMessage());
            return "";
        }
    }
}