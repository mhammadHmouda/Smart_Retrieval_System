package edu.najah.stu.ir.project.services;

import edu.najah.stu.ir.project.models.GeoPoint;
import edu.najah.stu.ir.project.models.GeoReference;
import edu.najah.stu.ir.project.models.TemporalExpression;
import kong.unirest.Unirest;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class GeoAndTemporalExtraction {
    private TokenNameFinderModel locationModel;
    private TokenNameFinderModel dateModel;
    private static final Map<String, GeoPoint> cache = new HashMap<>();

    public GeoAndTemporalExtraction() {
        try {
            InputStream locationModelIn = new FileInputStream("src/main/resources/models/en-ner-location.bin");
            locationModel = new TokenNameFinderModel(locationModelIn);

            InputStream dateModelIn = new FileInputStream("src/main/resources/models/en-ner-date.bin");
            dateModel = new TokenNameFinderModel(dateModelIn);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<GeoReference> extractGeoReferences(String text) {
        List<String> references = extract(locationModel, text);

        List<GeoReference> geoReferences = new ArrayList<>();
        for (String reference : references) {
            geoReferences.add(new GeoReference(reference));
        }

        return geoReferences;
    }

    public List<TemporalExpression> extractTemporalExpression(String text) {
        List<String> expressions = extract(dateModel, text);

        List<TemporalExpression> temporalExpressions = new ArrayList<>();
        for (String expression : expressions) {
            temporalExpressions.add(new TemporalExpression(expression));
        }

        return temporalExpressions;
    }
    private List<String> extract(TokenNameFinderModel model, String text) {
        NameFinderME finder = new NameFinderME(model);
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(text);
        Span[] spans = finder.find(tokens);

        if (spans.length == 0) {
            return new ArrayList<>();
        }

        List<String> expressions = new ArrayList<>();
        for (Span span : spans) {
            String expression = String.join(" ", Arrays.copyOfRange(tokens, span.getStart(), span.getEnd()));
            expressions.add(expression);
        }

        return expressions;
    }

    public GeoPoint getGeoPoint(List<GeoReference> references) {

        GeoPoint geoPoint = new GeoPoint(0, 0);

        try {
            for (GeoReference reference : references) {
                String address = reference.reference();

                if (cache.containsKey(address.toLowerCase())) {
                    return cache.get(address);
                }

                String apiUrl = "https://nominatim.openstreetmap.org/search?format=json&q="
                        + address.replaceAll("\\s+", "%20");

                var response = Unirest.get(apiUrl).header("accept", "application/json").asJson();

                if (response.getStatus() == 200) {
                    var result = response.getBody().getArray().getJSONObject(0);
                    double latitude = result.getDouble("lat");
                    double longitude = result.getDouble("lon");

                    cache.put(address.toLowerCase(), new GeoPoint(latitude, longitude));
                    return new GeoPoint(latitude, longitude);
                }
            }

            return geoPoint;
        } catch (Exception e){
            return geoPoint;
        }
    }
}