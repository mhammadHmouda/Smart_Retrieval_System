package edu.najah.stu.ir.project.controllers;

import edu.najah.stu.ir.project.models.DateAggResponse;
import edu.najah.stu.ir.project.models.GeoRefResponse;
import edu.najah.stu.ir.project.models.MultipleFactor;
import edu.najah.stu.ir.project.models.ReuterDocument;
import edu.najah.stu.ir.project.services.ESService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reuters")
@RequiredArgsConstructor
public class ESController {

    private final ESService service;

    @GetMapping("/query/autocomplete")
    public ResponseEntity<?> findByTitle(@RequestParam String title) {
        List<String> documents = service.findByTitle(title);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/query/multiple")
    public ResponseEntity<?> findByMultipleFactor(@RequestBody MultipleFactor multipleFactor) {
        List<ReuterDocument> documents = service.findByMultipleFactor(multipleFactor);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/query/geo")
    public ResponseEntity<?> findTop10GeoReferences() {
        List<GeoRefResponse> geoReferences = service.findTop10GeoReferences();
        return ResponseEntity.ok(geoReferences);
    }

    @GetMapping("/query/time")
    public ResponseEntity<?> findDistributionOverTime() {
        List<DateAggResponse> dateAggResponses = service.findDistributionOverTime();
        return ResponseEntity.ok(dateAggResponses);
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadDocuments() {
        service.loadDocumentToIndex();
        return ResponseEntity.ok("Documents loaded successfully!");
    }

    @DeleteMapping
    public ResponseEntity<?> deleteIndex() {
        String result = service.deleteIndex();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> addDocument(@RequestBody ReuterDocument document) {
        String id = service.addDocumentToIndex(document);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocumentById(@PathVariable String id) {
        String deletedId = service.deleteById(id);
        return ResponseEntity.ok("Document with id: ( " + deletedId + " ) deleted successfully!");
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        List<ReuterDocument> persons = service.findAll();
        return ResponseEntity.ok(persons);
    }

    @GetMapping("/query/{id}")
    public ResponseEntity<?> findById(@PathVariable int id) {
        ReuterDocument document = service.findDocumentById(id);
        return ResponseEntity.ok(document);
    }
}