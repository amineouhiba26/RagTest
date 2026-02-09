package com.example.demo.controller;

import com.example.demo.service.VectorDocumentIngestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final VectorDocumentIngestionService vectorIngestionService;

    public DocumentController(VectorDocumentIngestionService vectorIngestionService) {
        this.vectorIngestionService = vectorIngestionService;
    }

    @PostMapping("/vector/insurance")
    public String ingestInsuranceToVector() {
        try {
            String filePath = System.getProperty("user.dir") + "/Insurance - Wikipedia.pdf";
            vectorIngestionService.ingestPdfToVectorStore(filePath);
            return "Insurance PDF successfully ingested to vector database for precise RAG!";
        } catch (Exception e) {
            return "Error ingesting to vector database: " + e.getMessage();
        }
    }
    @PostMapping("/vector/upload")
    public String ingestAnyFileToVector(@RequestParam String fileName) {
        try {
            String filePath = System.getProperty("user.dir") + fileName;

            vectorIngestionService.ingestPdfToVectorStore(filePath);
            return " PDF successfully ingested to vector database for precise RAG!";
        } catch (Exception e) {
            return "Error ingesting to vector database: " + e.getMessage();
        }
    }

}
