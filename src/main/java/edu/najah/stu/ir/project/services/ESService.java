package edu.najah.stu.ir.project.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import edu.najah.stu.ir.project.models.ReuterDocument;
import edu.najah.stu.ir.project.utils.ReuterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ESService {

    @Value("${es.index.name}")
    private String indexName;

    private final ElasticsearchClient es;

    private final ReuterUtils reuterUtils;

    public List<String> findByTitle(String title) {
        try {
            var response = es.search(s -> s.index(indexName)
                    .query(q -> q.match(b -> b.field("title").query(title).fuzziness("1")
                    )).size(10), ReuterDocument.class);

            return response.hits().hits()
                    .stream().map(hit -> {
                        assert hit.source() != null;
                        return hit.source().getTitle();
                    }).toList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void loadDocumentToIndex() {
        Instant start = Instant.now();
        try {
            List<File> sgmFiles = getSGMFiles();

            for (File sgmFile : sgmFiles) {
                List<ReuterDocument> documents = reuterUtils.processFile(sgmFile);
                createAndExecuteBulkRequest(documents);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toSeconds();
        System.out.println("Time elapsed: " + timeElapsed + " seconds");
    }

    private void createAndExecuteBulkRequest(List<ReuterDocument> documents) {
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (ReuterDocument reuter : documents) {
                br.operations(op -> op.index(idx -> idx
                        .index(indexName)
                        .id(String.valueOf(reuter.getId()))
                        .document(reuter)));
            }

            executeBulkRequest(br);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private List<File> getSGMFiles() throws Exception {
        File folder = new File("src/main/resources/data");
        File[] sgmFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".sgm"));

        if (sgmFiles == null || sgmFiles.length == 0) {
            throw new Exception("No .sgm files found in the specified folder.");
        }

        return List.of(sgmFiles);
    }

    private void executeBulkRequest(BulkRequest.Builder br) throws IOException {
        BulkResponse result = es.bulk(br.build());

        if (result.errors()) {
            System.err.println("Bulk request failed: ");
        } else {
            System.out.println("Bulk request successful");
        }
    }

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
            var response = es.index(c -> c.index(indexName).document(document).id(String.valueOf(document.getId())));
            return response.id();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public List<ReuterDocument> findAll() {
        try {
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