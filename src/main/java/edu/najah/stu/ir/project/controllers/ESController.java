package edu.najah.stu.ir.project.controllers;

import edu.najah.stu.ir.project.models.ReuterDocument;
import edu.najah.stu.ir.project.services.ESService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/reuters")
@RequiredArgsConstructor
public class ESController {

    private final ESService service;

    @GetMapping("/docs/query/geo")
    public ResponseEntity<?> findTop10GeoReferences() {
        return ResponseEntity.ok(service.findTop10GeoReferences());
    }

    @GetMapping("/docs/query/time")
    public ResponseEntity<?> findDistributionOverTime() {
        return ResponseEntity.ok(service.findDistributionOverTime());
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

    @PostMapping("/docs")
    public ResponseEntity<?> addDocument(@RequestBody ReuterDocument document) {
        String id = service.addDocumentToIndex(document);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/docs/{id}")
    public ResponseEntity<?> deleteDocumentById(@PathVariable String id){
        String deletedId = service.deleteById(id);
        return ResponseEntity.ok("Document with id: ( " + deletedId + " ) deleted successfully!");
    }

    @GetMapping("/docs")
    public ResponseEntity<?> findAll() {
        List<ReuterDocument> persons = service.findAll();
        return  ResponseEntity.ok(persons);
    }

    @GetMapping("/docs/query")
    public ResponseEntity<?> findByTitle(@RequestParam String title) {
        List<String> documents = service.findByTitle(title);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/docs/query/{id}")
    public ResponseEntity<?> findById(@PathVariable int id) {
        ReuterDocument document = service.findDocumentById(id);
        return  ResponseEntity.ok(document);
    }
}