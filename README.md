# 🤖 AI-Powered Insurance Claim Processing System

A sophisticated multi-agent AI system built with **Spring Boot**, **LangChain4j**, **Spring AI**, and **Ollama** that automates insurance claim processing using RAG (Retrieval-Augmented Generation) and the **Orchestrator-Workers pattern**.

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.34.0-FF6B6B?style=for-the-badge)](https://docs.langchain4j.dev/)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.1.1-6DB33F?style=for-the-badge)](https://spring.io/projects/spring-ai)
[![Ollama](https://img.shields.io/badge/Ollama-Latest-000000?style=for-the-badge&logo=ollama)](https://ollama.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-316192?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)

---

## 🌟 Key Features

### 🔥 **Dual AI Framework Integration**
- **LangChain4j 0.34.0**: Advanced AI orchestration and RAG capabilities
- **Spring AI 1.1.1**: Vector store integration and document processing
- **Seamless coexistence**: Both frameworks working together without conflicts

### 🤖 **Multi-Agent Insurance Processing**
- **Agent Routeur**: Intelligent claim classification (8 insurance types)
- **Agent Validateur**: RAG-powered policy compliance validation  
- **Agent Estimateur**: Multimodal cost estimation with photo analysis
- **Orchestrator**: Asynchronous workflow coordination

### 📊 **RAG (Retrieval-Augmented Generation)**
- **Document ingestion**: PDF parsing and vector embedding
- **Semantic search**: Find relevant policy information
- **Context-aware responses**: Answers based only on uploaded documents
- **Dual vector stores**: In-memory (LangChain4j) + PostgreSQL (Spring AI)

### 🔄 **Advanced Architecture Patterns**
- **Orchestrator-Workers**: Coordinated multi-agent processing
- **Asynchronous processing**: CompletableFuture-based workflows
- **Error handling**: Circuit breaker patterns and graceful degradation
- **Monitoring**: Comprehensive logging and metrics

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     REST API Endpoints                           │
│   /api/agents/* | /rag/* | /documents/* | /langchain4j/*       │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Multi-Agent Orchestrator                        │
│              (OrchestratorMultiAgents)                          │
│  • Workflow management    • Error handling                      │
│  • Agent coordination     • Status tracking                     │
└──────┬──────────────┬─────────────────┬────────────────────────┘
       │              │                 │
       ▼              ▼                 ▼
┌──────────┐   ┌─────────────┐   ┌────────────┐
│  Agent   │   │   Agent     │   │   Agent    │
│ Routeur  │   │ Validateur  │   │ Estimateur │
│(Classify)│   │(Validate)   │   │(Estimate)  │
└────┬─────┘   └──────┬──────┘   └─────┬──────┘
     │                │                 │
     │                ▼                 │
     │         ┌──────────────────┐     │
     │         │  RAG System      │     │
     │         │  • LangChain4j   │     │
     │         │  • Spring AI     │     │
     │         │  • Vector Search │     │
     │         └──────────────────┘     │
     │                                  │
     └──────────────┬───────────────────┘
                    ▼
    ┌────────────────────────────────────┐
    │         AI Infrastructure          │
    │  • Ollama (llama3.2:latest)      │
    │  • PostgreSQL + PgVector          │
    │  • Document Processing            │
    └────────────────────────────────────┘
```

---

## 🚀 Quick Start Guide

### Prerequisites

Make sure you have these installed:
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop/))
- **Ollama** ([Download](https://ollama.com/))  
- **Java 17+**
- **Maven 3.6+**

### 1. Start PostgreSQL Database with Vector Support

```bash
# Stop any existing container
docker stop pgvector && docker rm pgvector

# Start fresh PostgreSQL with PgVector extension
docker run --name pgvector \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=testrag \
  -p 5432:5432 -d \
  pgvector/pgvector:pg16
```

### 2. Set Up AI Models (Ollama)

```bash
# Install the chat model (LLM)
ollama run llama3.2

# Install the embedding model (for vector search)  
ollama pull nomic-embed-text:latest

# Verify models are installed
ollama list
```

### 3. Run the Application

```bash
# Clone and navigate to project
git clone <repository-url>
cd demo

# Start the application
./mvnw spring-boot:run
```

Wait for the message: `Started DemoApplication in X.XXX seconds`

### 4. Load Sample Insurance Documents

```bash
# Ingest PDF to Spring AI vector store
curl -X POST "http://localhost:8080/documents/vector/insurance"

# Ingest PDF to LangChain4j vector store  
curl -X POST "http://localhost:8080/langchain4j/ingest"
```

---

## 📋 Multi-Agent System Explained

### 🎯 Agent Routeur (Classification Agent)

**Purpose**: Automatically classifies insurance claims into 8 categories

**Supported Claim Types**:
- `ACCIDENT_AUTO` - Vehicle accidents
- `DEGAT_EAU` - Water damage, floods, leaks
- `INCENDIE` - Fire, explosions, smoke damage  
- `VOL` - Theft, burglary
- `VANDALISME` - Intentional property damage
- `BRIS_GLACE` - Glass breakage (windshields, windows)
- `CATASTROPHE_NATURELLE` - Natural disasters (storms, hail)
- `AUTRES` - Other claim types

**How it works**:
```java
// Uses LangChain4j ChatModel for intelligent classification
String prompt = """
    Analyze this claim description and classify it:
    "%s"
    
    Available types: ACCIDENT_AUTO, DEGAT_EAU, INCENDIE...
    Respond with the exact type name only.
    """;
```

### 📋 Agent Validateur (Compliance Agent)

**Purpose**: Validates claims against insurance policy using RAG

**Validation Criteria**:
1. ✅ Claim type covered by policy
2. ✅ Circumstances meet policy conditions  
3. ✅ No exclusions apply
4. ✅ Declaration timelines respected
5. ✅ Sufficient information provided

**RAG Integration**:
```java
// Queries policy database using RAG
String policyInfo = ragService.getInformation(
    "What are coverage conditions for " + claimType + "?"
);

// AI validates against retrieved policy information
boolean compliant = !response.contains("NON_CONFORME");
```

### 💰 Agent Estimateur (Cost Estimation Agent)

**Purpose**: Estimates repair/replacement costs using multimodal analysis

**Analysis Components**:
- **Damage Assessment**: Type and extent of damage
- **Cost Calculation**: Parts, labor, indirect costs
- **Photo Analysis**: Simulated multimodal analysis of damage photos
- **Industry Standards**: Comparison with standard repair costs

**Estimation Process**:
```java
// Analyzes damage photos (simulated - ready for GPT-4V integration)
String photoAnalysis = analyzePhotos(claim.getPhotos());

// Generates detailed cost estimate
String prompt = """
    As an insurance cost estimator, calculate estimate for:
    Type: %s | Description: %s | Photo analysis: %s
    
    Consider: damage extent, repair costs, labor, materials, indirect costs
    Format: [AMOUNT] | [JUSTIFICATION] | [CONFIDENCE]
    """;
```

### 🎭 Orchestrator (Workflow Coordinator)

**Purpose**: Manages the complete claim processing workflow

**Workflow Steps**:
1. **Phase 1**: Classification → `Agent Routeur`
2. **Phase 2**: Validation → `Agent Validateur`  
3. **Phase 3**: Cost estimation → `Agent Estimateur`
4. **Phase 4**: Report generation and decision

**Asynchronous Processing**:
```java
// Non-blocking sequential execution
return CompletableFuture
    .supplyAsync(() -> routerAgent.classify(claim))
    .thenCompose(classified -> 
        validatorAgent.validateAsync(classified))
    .thenCompose(validated -> 
        estimatorAgent.estimateAsync(validated));
```

---

## 🔍 RAG System Deep Dive

### What is RAG?

**RAG (Retrieval-Augmented Generation)** prevents AI hallucinations by:
1. **Retrieving** relevant documents from your database
2. **Augmenting** the AI prompt with retrieved context
3. **Generating** responses based only on provided information

### Dual RAG Implementation

#### 🔵 LangChain4j RAG Pipeline
```java
// Document ingestion
Document doc = parser.parse(pdfFile);
List<TextSegment> segments = splitter.split(doc);
embeddingStore.addAll(embeddings, segments);

// Query processing  
List<Content> relevant = contentRetriever.retrieve(query);
String context = buildContext(relevant);
String response = chatModel.generate(prompt + context);
```

#### 🟢 Spring AI RAG Pipeline
```java
// Vector storage in PostgreSQL
@Autowired VectorStore vectorStore;
vectorStore.add(createDocuments(pdfContent));

// Similarity search
List<Document> similar = vectorStore.similaritySearch(
    SearchRequest.query(question).withTopK(5)
);
```

### Document Processing Pipeline

1. **PDF Parsing**: Extract text using Apache Tika/PDFBox
2. **Text Chunking**: Split into 500-character segments with 100-char overlap
3. **Embedding Generation**: Convert text to 768-dimensional vectors
4. **Vector Storage**: Store in PostgreSQL (Spring AI) + In-Memory (LangChain4j)
5. **Retrieval**: Semantic search for relevant segments
6. **Generation**: AI response using retrieved context

---

## 🌐 API Reference

### Multi-Agent Endpoints

#### System Status
```http
GET /api/agents/status
```
**Response**:
```json
{
  "service": "Multi-Agents Orchestrator",
  "status": "Operational", 
  "agents": ["Routeur", "Validateur", "Estimateur"],
  "patterns": "Orchestrator-Workers",
  "capabilities": [
    "Automatic claim classification",
    "RAG-based compliance validation", 
    "Multimodal cost estimation",
    "Detailed reporting",
    "Asynchronous processing"
  ]
}
```

#### Process Insurance Claim
```http
POST /api/agents/traiter-sinistre
Content-Type: application/json

{
  "email": "client@example.com",
  "description": "Car accident on highway A7, front collision with airbag deployment",
  "photos": ["front_damage.jpg", "airbag_deployed.jpg"]
}
```

**Response**:
```json
{
  "success": true,
  "demandeId": "uuid-here",
  "statut": "ESTIME",
  "typeSinistre": "Accident automobile", 
  "conforme": true,
  "estimationCout": 4250.00,
  "commentaires": "Complete damage assessment with airbag replacement"
}
```

#### Express Processing
```http
POST /api/agents/traiter-express
```
Simplified workflow for urgent claims.

#### Generate Report
```http
POST /api/agents/generer-rapport/{claimId}
```
Detailed claim processing report.

### RAG Endpoints

#### Spring AI RAG
```http
# Ingest document
POST /documents/vector/insurance

# Ask question
GET /rag/ask?question=What is the deductible for auto insurance?
```

#### LangChain4j RAG  
```http  
# Ingest document
POST /langchain4j/ingest

# Ask question
GET /langchain4j/ask?question=What are the coverage limits?

# Simple chat
GET /langchain4j/chat?message=Hello
```

---

## ⚙️ Configuration

### Application Properties
```properties
# Application
spring.application.name=demo

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/testrag
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

# Spring AI - Ollama Integration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:latest
spring.ai.ollama.embedding.options.model=nomic-embed-text:latest
spring.ai.vectorstore.pgvector.initialize-schema=true

# LangChain4j - Ollama Integration  
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2:latest
langchain4j.ollama.embedding-model.base-url=http://localhost:11434
langchain4j.ollama.embedding-model.model-name=nomic-embed-text:latest

# Document Processing
app.pdf.chunk-size=500
app.pdf.chunk-overlap=100
```

### Docker Compose (Optional)
```yaml
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: testrag
      POSTGRES_USER: postgres  
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
      
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
```

---

## 🧪 Testing Examples

### Test Auto Claim Processing
```bash
curl -X POST "http://localhost:8080/api/agents/traiter-sinistre" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "description": "Rear-end collision at traffic light. Bumper damage and trunk deformation.",
    "photos": ["rear_damage.jpg", "bumper_close.jpg"]
  }'
```

### Test Water Damage Claim  
```bash
curl -X POST "http://localhost:8080/api/agents/traiter-sinistre" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "homeowner@example.com", 
    "description": "Kitchen flooding from burst pipe. Damaged flooring and cabinets.",
    "photos": ["flooded_kitchen.jpg", "water_damage.jpg"]
  }'
```

### Test RAG System
```bash
# Upload insurance document
curl -X POST "http://localhost:8080/documents/vector/insurance"

# Ask about coverage
curl -X GET "http://localhost:8080/rag/ask?question=What is covered under comprehensive auto insurance?"

# Test LangChain4j
curl -X POST "http://localhost:8080/langchain4j/ingest" 
curl -X GET "http://localhost:8080/langchain4j/ask?question=What are the exclusions for theft coverage?"
```

---

## 🔧 Technical Implementation Details

### Framework Integration Strategy

**Bean Isolation**: Prevents conflicts between Spring AI and LangChain4j
```java
@Configuration
public class OllamaConfig {
    
    @Bean("langchain4jChatModel") 
    @Qualifier("langchain4j")
    public ChatLanguageModel langChain4jChatModel() {
        return OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("llama3.2:latest")
            .build();
    }
    
    // Spring AI beans use different names
    @Bean("springAiChatModel")
    public ChatClient springAiChatClient() { ... }
}
```

### Asynchronous Processing
```java
@Service
public class OrchestratorMultiAgents {
    
    private final ExecutorService executor = 
        Executors.newFixedThreadPool(10);
    
    public CompletableFuture<ClaimResult> processAsync(Claim claim) {
        return CompletableFuture
            .supplyAsync(() -> classify(claim), executor)
            .thenCompose(this::validateAsync)
            .thenCompose(this::estimateAsync)
            .exceptionally(this::handleError);
    }
}
```

### Error Handling & Resilience
- **Circuit Breaker**: Prevents cascade failures
- **Retry Logic**: Exponential backoff for transient errors  
- **Fallback Mechanisms**: Default responses when AI unavailable
- **Graceful Degradation**: Partial functionality during outages

### Performance Optimizations
- **Connection Pooling**: Optimized DB and AI API connections
- **Caching**: RAG results cached for repeated queries
- **Thread Management**: Dedicated executors for agent tasks
- **Memory Management**: Efficient vector storage and retrieval

---

## 📊 Monitoring & Observability

### Logging
- **Agent Activities**: Detailed logs for each agent action
- **Workflow Tracking**: End-to-end claim processing traces  
- **Performance Metrics**: Processing times and throughput
- **Error Tracking**: Comprehensive error logging and alerting

### Metrics (Ready for Micrometer/Prometheus)
```java
@Component 
public class AgentMetrics {
    private final Counter claimsProcessed;
    private final Timer processingTime;
    private final Gauge activeAgents;
}
```

---

## 🚀 Production Deployment

### Scalability Considerations
- **Horizontal Scaling**: Multiple application instances behind load balancer
- **Database Scaling**: Read replicas for vector similarity searches
- **AI Model Scaling**: Multiple Ollama instances for high throughput
- **Caching Layer**: Redis for cross-instance data sharing

### Security Best Practices  
- **API Authentication**: JWT tokens or API keys
- **Data Encryption**: TLS for all communications
- **Access Control**: Role-based permissions for agents
- **Audit Trail**: Immutable claim processing logs

### High Availability Setup
```yaml
# Production docker-compose.yml
version: '3.8'
services:
  app:
    image: insurance-ai:latest
    deploy:
      replicas: 3
    
  postgres:
    image: pgvector/pgvector:pg16
    deploy:
      replicas: 2
    
  ollama:
    image: ollama/ollama:latest
    deploy:
      replicas: 2
```

---

## 🛠️ Development & Extension

### Adding New Agent Types
```java
@Service
public class AgentFraudeDetection {
    
    @Autowired
    private ChatLanguageModel chatModel;
    
    public FraudResult detectFraud(Claim claim) {
        // Implement fraud detection logic
        String prompt = buildFraudDetectionPrompt(claim);
        return parseFraudResponse(chatModel.generate(prompt));
    }
}
```

### Custom RAG Implementations
```java
@Service
public class CustomRagService {
    
    public String queryWithCustomLogic(String question) {
        // Custom retrieval logic
        List<Document> relevant = customRetrieval(question);
        
        // Custom generation logic  
        return customGeneration(question, relevant);
    }
}
```

### Integration with External Systems
- **CRM Integration**: Customer data synchronization
- **Payment Processing**: Automated claim payouts
- **Document Management**: Integration with document management systems
- **Notification Service**: Real-time claim status updates

---

## 🆘 Troubleshooting

### Common Issues

#### Database Connection
```bash
# Check PostgreSQL status
docker ps | grep pgvector

# Test connection
psql -h localhost -U postgres -d testrag
```

#### Ollama Models
```bash
# Verify Ollama is running
curl http://localhost:11434/api/tags

# Pull missing models
ollama pull llama3.2:latest
ollama pull nomic-embed-text:latest
```

#### Agent Processing Errors
```bash
# Check application logs
tail -f logs/application.log

# Test individual agent endpoints
curl -X GET "http://localhost:8080/api/agents/status"
```

### Performance Tuning

#### JVM Settings
```bash
# Production JVM flags
-Xmx8g -Xms4g 
-XX:+UseG1GC 
-XX:MaxGCPauseMillis=200
```

#### Database Optimization  
```sql
-- Optimize vector searches
CREATE INDEX CONCURRENTLY ON vector_store 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);
```

---

## 📚 Additional Resources

### Official Documentation
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Spring AI Reference](https://spring.io/projects/spring-ai) 
- [Ollama Model Library](https://ollama.com/library)
- [PgVector Extension](https://github.com/pgvector/pgvector)

### Learning Resources
- [RAG Fundamentals](https://arxiv.org/abs/2005.11401)
- [Multi-Agent Systems](https://en.wikipedia.org/wiki/Multi-agent_system)
- [Vector Databases Guide](https://www.pinecone.io/learn/vector-database/)

---

## 🤝 Contributing

We welcome contributions! Please see our contributing guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`) 
5. **Open** a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **LangChain4j Team** for the excellent AI orchestration framework
- **Spring Team** for Spring AI and Spring Boot
- **Ollama Team** for local LLM deployment made easy  
- **PostgreSQL & PgVector** for powerful vector database capabilities

---

**Built with ❤️ using Spring Boot, LangChain4j, Spring AI, and Ollama**

*This system demonstrates the power of combining multiple AI frameworks to build sophisticated, production-ready applications that can handle complex business workflows with intelligence and reliability.*
