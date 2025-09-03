#!/usr/bin/env python3
"""
LangChain RAG (Retrieval Augmented Generation) Example using Groq.
This example demonstrates how to create a simple document-based QA system.
"""

import os
from dotenv import load_dotenv
from langchain.chains import RetrievalQA
from langchain_community.document_loaders import TextLoader
from langchain.text_splitter import CharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_groq import ChatGroq
from langchain_huggingface import HuggingFaceEmbeddings

def create_sample_documents():
    """Create sample documents for the RAG example."""
    documents = [
        {
            "content": """
            LangChain is a powerful framework for building applications with large language models (LLMs).
            It provides abstractions and tools to easily work with various LLM providers, create chains of 
            reasoning, and integrate external data sources. LangChain supports both Python and JavaScript 
            implementations and is widely used for building chatbots, question-answering systems, and 
            document analysis tools.
            """,
            "metadata": {"source": "langchain_intro.txt"}
        },
        {
            "content": """
            Retrieval Augmented Generation (RAG) is a technique that combines the power of large language 
            models with external knowledge sources. Instead of relying solely on the model's training data,
            RAG systems first retrieve relevant information from a knowledge base or document collection,
            then use that information to generate more accurate and contextual responses. This approach
            helps reduce hallucinations and provides more up-to-date information.
            """,
            "metadata": {"source": "rag_explanation.txt"}
        },
        {
            "content": """
            Vector databases and embeddings are crucial components of modern AI applications. Embeddings
            convert text into numerical vectors that capture semantic meaning, allowing computers to 
            understand relationships between different pieces of text. Vector databases like FAISS, Pinecone,
            and Weaviate store these embeddings efficiently and enable fast similarity searches, making
            them perfect for RAG applications and semantic search systems.
            """,
            "metadata": {"source": "vector_databases.txt"}
        }
    ]
    return documents

def main():
    # Load environment variables
    load_dotenv()
    
    print("🔍 LangChain RAG Example - Document Q&A System with Groq")
    print("=" * 60)
    
    # Initialize embeddings and LLM with Groq
    embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
    llm = ChatGroq(
        groq_api_key=os.getenv("GROQ_API_KEY"),
        model_name="deepseek-r1-distill-llama-70b",
        temperature=0.2
    )
    
    # Create sample documents
    sample_docs = create_sample_documents()
    print(f"📚 Created {len(sample_docs)} sample documents")
    
    # Split documents into chunks
    text_splitter = CharacterTextSplitter(
        chunk_size=500,
        chunk_overlap=50
    )
    
    # Create document objects
    from langchain.schema import Document
    documents = []
    for doc in sample_docs:
        documents.append(Document(
            page_content=doc["content"].strip(),
            metadata=doc["metadata"]
        ))
    
    # Split documents
    texts = text_splitter.split_documents(documents)
    print(f"📄 Split into {len(texts)} text chunks")
    
    try:
        # Create vector store
        print("🔧 Creating vector store with embeddings...")
        vectorstore = FAISS.from_documents(texts, embeddings)
        
        # Create retrieval QA chain
        qa_chain = RetrievalQA.from_chain_type(
            llm=llm,
            chain_type="stuff",
            retriever=vectorstore.as_retriever(search_kwargs={"k": 2}),
            return_source_documents=True
        )
        
        # Example questions
        questions = [
            "What is LangChain and what is it used for?",
            "How does RAG help with language models?",
            "What are vector databases and why are they important?",
            "What programming languages does LangChain support?"
        ]
        
        print("\n💡 Example Questions:")
        for i, q in enumerate(questions, 1):
            print(f"{i}. {q}")
        
        print("\n" + "=" * 60)
        print("Ask questions about the documents (type 'quit' to exit):")
        print("=" * 60)
        
        while True:
            try:
                question = input("\n❓ Question: ").strip()
                
                if question.lower() in ['quit', 'exit', 'bye', '']:
                    print("👋 Goodbye!")
                    break
                
                # Get answer from RAG chain
                result = qa_chain({"query": question})
                
                print(f"\n✨ Answer: {result['result']}")
                
                # Show source documents
                if result['source_documents']:
                    print("\n📚 Sources:")
                    for i, doc in enumerate(result['source_documents'], 1):
                        source = doc.metadata.get('source', 'Unknown')
                        content_preview = doc.page_content[:100] + "..."
                        print(f"  {i}. {source}: {content_preview}")
                
                print("-" * 60)
                
            except KeyboardInterrupt:
                print("\n👋 Session ended!")
                break
            except Exception as e:
                print(f"❌ Error: {str(e)}")
                
    except Exception as e:
        print(f"❌ Error setting up RAG system: {str(e)}")
        print("This might be due to Groq API key issues. Make sure GROQ_API_KEY is set in your .env file.")

if __name__ == "__main__":
    main()