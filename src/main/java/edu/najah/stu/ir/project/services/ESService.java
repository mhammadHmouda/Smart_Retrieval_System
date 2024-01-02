package edu.najah.stu.ir.project.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import edu.najah.stu.ir.project.models.DateAggResponse;
import edu.najah.stu.ir.project.models.GeoRefResponse;
import edu.najah.stu.ir.project.models.MultipleFactor;
import edu.najah.stu.ir.project.models.ReuterDocument;
import edu.najah.stu.ir.project.utils.ReuterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ESService {

    @Value("${es.index.name}")
    private String indexName;

    @Value("${documents.path}")
    private String reutersDocumentsPath;

    private final ElasticsearchClient es;

    private final ReuterUtils reuterUtils;


    public List<String> findByTitle(String title) {
        try {
            var response = es.search(s -> s.index(indexName)
                    .query(q -> q.match(b -> b.field("title").query(title)))
                    .size(10), ReuterDocument.class);

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

    public List<ReuterDocument> findByMultipleFactor(MultipleFactor factors) {
        try {
            var response = es.search(s -> s.index(indexName)
                            .query(q -> q.bool(b -> b.must(m -> m.functionScore(f -> f
                                    .query(q2 -> q2.multiMatch(b2 -> b2.fields(List.of("title^3", "content")).query(factors.query())))
                                    .functions(List.of(
                                            FunctionScore.of(f2 -> f2.filter(f3 -> f3.nested(n -> n.path("geo_references")
                                                    .query(q3 -> q3.match(t -> t.field("geo_references.reference")
                                                            .query(factors.geoReference()))))).weight(3d)),

                                            FunctionScore.of(f3 -> f3.filter(f4 -> f4.nested(n2 -> n2.path("temporal_expressions")
                                                    .query(q4 -> q4.matchPhrasePrefix(t2 -> t2.field("temporal_expressions.expression")
                                                            .query(factors.temporalExpression()))))).weight(2d)))
                                    )
                                    .scoreMode(FunctionScoreMode.Sum)
                                    .boostMode(FunctionBoostMode.Sum)
                            ))))
                            .sort(List.of(SortOptions.of(s2 -> s2.field(f -> f.field("_score").order(SortOrder.Desc))),
                                    SortOptions.of(s2 -> s2.field(f -> f.field("publication_date").order(SortOrder.Desc)))))
                    , ReuterDocument.class);

            return reuterUtils.mapToReuters(response);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<GeoRefResponse> findTop10GeoReferences() {
        try {
            var response = es.search(s -> s.index(indexName).size(0)
                            .aggregations("geo_references", ag -> ag
                                    .nested(nes -> nes.path("geo_references")).aggregations("geo_references",
                                            ag2 -> ag2.terms(t -> t.field("geo_references.reference.keyword")
                                                    .size(10)))), Void.class);

            StringTermsAggregate termsAgg = response.aggregations().get("geo_references")
                    .nested().aggregations().get("geo_references").sterms();

            return termsAgg.buckets().array().stream()
                    .map(bucket -> new GeoRefResponse(bucket.key().stringValue(), bucket.docCount()))
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    public List<DateAggResponse> findDistributionOverTime(){
        try {
            var response = es.search(s -> s.index(indexName)
                            .aggregations("dates", ag -> ag.dateHistogram(t -> t.field("publication_date")
                            .fixedInterval(f -> f.time("1d")).format("yyyy-MM-dd").minDocCount(1))), Void.class);

            return response.aggregations().get("dates")
                    .dateHistogram().buckets().array().stream()
                    .map(bucket -> new DateAggResponse(bucket.keyAsString(), bucket.docCount()))
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
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
            var response = es.index(c -> c.index(indexName)
                    .document(document).id(String.valueOf(document.getId())));

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
            return "N/A";
        }
    }

    public ReuterDocument findDocumentById(long id){
        try {
            var response = es.get(c -> c.index(indexName)
                            .id(String.valueOf(id)), ReuterDocument.class);

            return response.source();
        } catch (Exception e){
            System.out.println(e.getMessage());
            return new ReuterDocument();
        }
    }

    public void loadDocumentToIndex() {
        try {
            List<File> sgmFiles = getSGMFiles();

            sgmFiles.forEach(file -> {
                List<ReuterDocument> documents = reuterUtils.processFile(file);
                createAndExecuteBulkRequest(documents);
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void createAndExecuteBulkRequest(List<ReuterDocument> documents) {
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            documents.forEach(document -> br
                    .operations(op -> op.index(idx -> idx.index(indexName)
                    .id(String.valueOf(document.getId())).document(document))));

            executeBulkRequest(br);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private List<File> getSGMFiles() throws Exception {
        File folder = new File(reutersDocumentsPath);
        File[] sgmFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".sgm"));

        if (sgmFiles == null || sgmFiles.length == 0) {
            throw new Exception("No .sgm files found in the specified folder.");
        }

        return List.of(sgmFiles);
    }

    private void executeBulkRequest(BulkRequest.Builder br) throws IOException {
        BulkResponse result = es.bulk(br.build());

        if (result.errors()) {
            throw new IOException("Bulk request failed.");
        } else {
            System.out.println("Bulk request successful");
        }
    }
}