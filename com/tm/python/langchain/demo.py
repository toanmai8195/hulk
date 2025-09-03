#!/usr/bin/env python3
"""
Demo script to showcase LangChain functionality without requiring an API key.
This demonstrates LangChain's components and structure.
"""

from langchain.prompts import PromptTemplate
from langchain.schema import StrOutputParser
from langchain.schema.runnable import RunnablePassthrough

def main():
    print("🚀 LangChain Demo - Component Showcase")
    print("=" * 50)
    
    # 1. Prompt Template Demo
    print("\n📝 1. Prompt Template Example")
    print("-" * 30)
    
    prompt_template = PromptTemplate(
        input_variables=["topic", "style"],
        template="Write a {style} explanation about {topic}:"
    )
    
    # Show how prompt formatting works
    topics = [
        {"topic": "artificial intelligence", "style": "simple"},
        {"topic": "machine learning", "style": "technical"},
        {"topic": "neural networks", "style": "beginner-friendly"}
    ]
    
    for example in topics:
        formatted_prompt = prompt_template.format(**example)
        print(f"Topic: {example['topic']}")
        print(f"Style: {example['style']}")
        print(f"Prompt: {formatted_prompt}")
        print()
    
    # 2. Chain Components Demo (without LLM)
    print("🔗 2. LangChain Components Structure")
    print("-" * 40)
    
    # Create a simple processing chain structure
    prompt = PromptTemplate(
        input_variables=["question"],
        template="Question: {question}\nThought process:"
    )
    
    # Demonstrate chain composition concepts
    print("Chain Components:")
    print("1. Input: User question")
    print("2. Prompt Template: Formats the question")
    print("3. LLM: Processes the formatted prompt (would be here)")
    print("4. Output Parser: Formats the response")
    print("5. Output: Final answer")
    
    # 3. Document Processing Demo
    print("\n📄 3. Document Processing Concepts")
    print("-" * 38)
    
    # Show text splitting concepts
    sample_text = """
    LangChain is a framework for developing applications powered by language models.
    It enables applications that are context-aware and can reason about their actions.
    The framework consists of several components including prompt templates, chains,
    memory systems, and integrations with various data sources and APIs.
    """
    
    print("Sample Document:")
    print(sample_text.strip())
    
    # Simple text splitting simulation
    sentences = [s.strip() for s in sample_text.split('.') if s.strip()]
    print(f"\nText Splitting Result: {len(sentences)} chunks")
    for i, chunk in enumerate(sentences, 1):
        print(f"Chunk {i}: {chunk}.")
    
    # 4. Memory Concepts Demo
    print("\n🧠 4. Memory System Concepts")
    print("-" * 32)
    
    # Simulate conversation memory
    conversation_history = [
        {"role": "user", "message": "What is machine learning?"},
        {"role": "assistant", "message": "Machine learning is a subset of AI..."},
        {"role": "user", "message": "Can you give me an example?"},
        {"role": "assistant", "message": "Sure! A common example is email spam detection..."}
    ]
    
    print("Conversation Memory Structure:")
    for i, turn in enumerate(conversation_history):
        print(f"{i+1}. {turn['role'].title()}: {turn['message'][:50]}...")
    
    # 5. RAG Concepts Demo
    print("\n🔍 5. RAG (Retrieval Augmented Generation) Concepts")
    print("-" * 55)
    
    # Knowledge base simulation
    knowledge_base = [
        {"id": 1, "content": "LangChain supports multiple LLM providers", "topic": "integrations"},
        {"id": 2, "content": "Chains can be composed to create complex workflows", "topic": "chains"},
        {"id": 3, "content": "Memory allows stateful conversations", "topic": "memory"},
        {"id": 4, "content": "Document loaders help ingest various file formats", "topic": "data"}
    ]
    
    user_query = "How does LangChain handle different data sources?"
    
    print(f"User Query: {user_query}")
    print("Knowledge Base Search Results:")
    
    # Simple keyword matching simulation
    relevant_docs = [doc for doc in knowledge_base if "data" in doc["topic"] or "integrations" in doc["topic"]]
    
    for doc in relevant_docs:
        print(f"- Document {doc['id']}: {doc['content']}")
    
    print(f"\nRAG Process:")
    print("1. User asks a question")
    print("2. System searches knowledge base for relevant documents")
    print("3. Retrieved documents provide context")
    print("4. LLM generates answer using both query and context")
    print("5. Response is more accurate and up-to-date")
    
    print("\n✅ Demo Complete!")
    print("This showcases LangChain's core concepts without needing API keys.")
    print("For live examples with LLMs, configure your .env file and run the other examples.")

if __name__ == "__main__":
    main()