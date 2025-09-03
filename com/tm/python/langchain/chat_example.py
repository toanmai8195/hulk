#!/usr/bin/env python3
"""
LangChain Chat Example demonstrating conversational AI with memory using Groq.
This example shows how to create a chatbot with conversation history.
"""

import os
from dotenv import load_dotenv
from langchain.chains import ConversationChain
from langchain.memory import ConversationBufferMemory
from langchain_groq import ChatGroq

def main():
    # Load environment variables
    load_dotenv()
    
    # Initialize the LLM with Groq
    llm = ChatGroq(
        groq_api_key=os.getenv("GROQ_API_KEY"),
        model_name="deepseek-r1-distill-llama-70b",
        temperature=0.8
    )
    
    # Create memory to store conversation history
    memory = ConversationBufferMemory()
    
    # Create a conversation chain with memory
    conversation = ConversationChain(
        llm=llm,
        memory=memory,
        verbose=True  # Shows the internal processing
    )
    
    print("🤖 LangChain Chat Example with Groq")
    print("=" * 50)
    print("Type 'quit', 'exit', or 'bye' to end the conversation")
    print("=" * 50)
    
    # Example conversation starters
    example_questions = [
        "What is your favorite programming language?",
        "Tell me about machine learning",
        "What did we just discuss?",
        "Can you remember what I asked first?"
    ]
    
    print("💡 Try these example questions:")
    for i, question in enumerate(example_questions, 1):
        print(f"{i}. {question}")
    print("-" * 50)
    
    while True:
        try:
            user_input = input("You: ").strip()
            
            if user_input.lower() in ['quit', 'exit', 'bye', '']:
                print("👋 Goodbye!")
                break
            
            # Get response from the conversation chain
            response = conversation.predict(input=user_input)
            # Handle both string and AIMessage responses
            if hasattr(response, 'content'):
                print(f"Bot: {response.content}")
            else:
                print(f"Bot: {response}")
            print("-" * 50)
            
        except KeyboardInterrupt:
            print("\n👋 Conversation ended!")
            break
        except Exception as e:
            print(f"❌ Error: {str(e)}")
            print("-" * 50)

if __name__ == "__main__":
    main()