package edu.najah.stu.ir.project.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
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
    private int operationCount = 0;

    private final ElasticsearchClient es;

    private final ReuterUtils reuterUtils;

    public String deleteIndex() {
        try {
            es.indices().delete(c -> c.index(indexName));
            return "Index deleted successfully!";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void loadDocumentToIndex() {
        try {
            List<ReuterDocument> documents = reuterUtils.readReuters();

            int batchSize = 50;

            BulkRequest.Builder br = new BulkRequest.Builder();

            for (ReuterDocument reuter : documents) {
                br.operations(op -> op.index(idx -> idx
                        .index(indexName)
                        .id(String.valueOf(reuter.getId()))
                        .document(reuter)));

                operationCount++;

                if (operationCount == batchSize) {
                    executeBulkRequest(br);
                    operationCount = 0;
                    br = new BulkRequest.Builder();
                }
            }

            executeBulkRequest(br);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void executeBulkRequest(BulkRequest.Builder br) throws IOException {
        if (operationCount > 0) {
            BulkResponse result = es.bulk(br.build());

            if (result.errors()) {
                System.err.println("Bulk request failed: ");
            } else {
                System.out.println("Bulk request successful");
            }
        }
    }

    public String addDocumentToIndex(ReuterDocument document) {
        try {
            var response = es.index(c -> c.index(indexName).document(document).id(String.valueOf(document.getId())));
            return response.id();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public List<ReuterDocument> findAll() {
        try {
//            Query byName = MatchQuery.of(m -> m.field("title").query("Hmouda"))._toQuery();
//            Query byId = MatchQuery.of(m -> m.field("id").query("2"))._toQuery();
//
//            var res = es.search(s -> s.index(indexName).query(q -> q.bool(b -> b.must(byName).must(byId))), ReuterDocument.class);
//            System.out.println(res.hits().hits().stream().map(Hit::source).toList().get(0));


            var response = es.search(s -> s.index(indexName)
                    .query(q -> q.matchAll(QueryBuilders.matchAll().build()))
                    .size(20), ReuterDocument.class);

            return reuterUtils.mapToReuters(response);
        } catch (Exception e) {
            e.printStackTrace();
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

    public ReuterDocument findDocumentById(long id){
        try {
            var response = es.get(c -> c.index(indexName).id(String.valueOf(id)), ReuterDocument.class);
            return response.source();
        } catch (Exception e){
            System.out.println(e.getMessage());
            return new ReuterDocument();
        }
    }
}