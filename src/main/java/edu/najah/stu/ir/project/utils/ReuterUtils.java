package edu.najah.stu.ir.project.utils;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import edu.najah.stu.ir.project.models.*;
import edu.najah.stu.ir.project.services.GeoAndTemporalExtraction;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Configuration;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Configuration
@RequiredArgsConstructor
public class ReuterUtils {

    private final GeoAndTemporalExtraction geoAndTemporalExtraction;

    public List<ReuterDocument> mapToReuters(SearchResponse<ReuterDocument> response) {
        return response.hits().hits()
                .stream().map(Hit::source)
                .toList();
    }

    public List<ReuterDocument> processFile(File sgmFile) {
        List<ReuterDocument> reuterDocuments = new ArrayList<>();

        try (Scanner scanner = new Scanner(sgmFile, StandardCharsets.UTF_8)) {
            String sgmContent = scanner.useDelimiter("\\A").next();
            Document xmlDoc = Jsoup.parse(sgmContent, "", Parser.xmlParser());

            Elements reutersElements = xmlDoc.select("REUTERS");
            for (Element reutersElement : reutersElements) {

                ReuterDocument reuterDocument = processReutersElement(reutersElement);

                long newId = Long.parseLong(reutersElement.attr("NEWID"));
                String lewisSplit = reutersElement.attr("LEWISSPLIT");
                reuterDocument.setId(newId);
                reuterDocument.setStatus(lewisSplit);

                if (!reuterDocument.getContent().contains("Blah blah blah.")) {
                    reuterDocuments.add(reuterDocument);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return reuterDocuments;
    }

    private ReuterDocument processReutersElement(Element reutersElement) throws Exception {

        Element title = reutersElement.selectFirst("TITLE");
        String titleText = title != null ? title.text() : "N/A";

        Element author = reutersElement.selectFirst("AUTHOR");
        String authorText = author != null ? author.text() : "N/A";
        List<Author> authors = extractAuthors(authorText);

        Element body = reutersElement.selectFirst("TEXT");
        String bodyText = body != null ? body.text() : "N/A";

        Element dateElement = reutersElement.selectFirst("DATE");
        String dateText = dateElement != null ? dateElement.text() : "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String formattedDate = sdf.format(getDate(dateText));

        List<TemporalExpression> temporalExpressions = geoAndTemporalExtraction.extractTemporalExpression(bodyText);
        List<GeoReference> geoReferencesFromText = geoAndTemporalExtraction.extractGeoReferences(bodyText);

        Element placesElement = reutersElement.selectFirst("PLACES");
        String placesText = placesElement != null ? placesElement.text() : "N/A";

        List<GeoReference> places = processPlaces(placesText);

        GeoPoint geoPoint = new GeoPoint(0, 0);
        if(!places.isEmpty()){
            geoPoint = geoAndTemporalExtraction.getGeoPoint(places);
        } else if (!geoReferencesFromText.isEmpty()) {
            geoPoint = geoAndTemporalExtraction.getGeoPoint(geoReferencesFromText);
        }

        ReuterDocument reuterDocument = new ReuterDocument();
        reuterDocument.setTitle(titleText);
        reuterDocument.setContent(bodyText);
        reuterDocument.setDate(formattedDate);
        reuterDocument.setAuthors(authors);
        reuterDocument.setGeoPoint(geoPoint);
        reuterDocument.setTemporalExpressions(temporalExpressions);
        reuterDocument.setGeoReferences(geoReferencesFromText);

        return reuterDocument;
    }

    private List<GeoReference> processPlaces(String placesText) {
        return Arrays.stream(placesText.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .map(GeoReference::new)
                .toList();
    }

    private Date getDate(String dateString) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d-MMM-yyyy HH:mm:ss.SS", Locale.US);
        return dateFormat.parse(dateString);
    }

    private List<Author> extractAuthors(String text) {
        Pattern pattern = Pattern.compile("By\\s+(.+?),\\s+Reuters", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        List<Author> authorsList = new ArrayList<>();

        if (matcher.find()) {
            var authors = matcher.group(1);
            if(authors.contains(" and ")) {
                for (String author: authors.split(" and ")) {
                    authorsList.add(getAuthorInfo(author));
                }
            } else {
                authorsList.add(getAuthorInfo(authors));
            }

            return authorsList;
        } else {
            return new ArrayList<>(){{
                add(new Author("N/A", "N/A"));
            }};
        }
    }

    private Author getAuthorInfo(String author) {
        String[] names = author.split("\\s+");
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[names.length - 1] : "";
        return new Author(firstName, lastName);
    }
}
