#!/usr/bin/env python3
"""
Simple LangChain example demonstrating basic chain creation and execution.
This example shows how to create a simple prompt template and chain with Groq.
"""

import os
from dotenv import load_dotenv
from langchain.prompts import PromptTemplate
from langchain_groq import ChatGroq

def main():
    # Load environment variables from .env file
    load_dotenv()
    
    # Initialize the LLM with Groq
    llm = ChatGroq(
        groq_api_key=os.getenv("GROQ_API_KEY"),
        model_name="deepseek-r1-distill-llama-70b",
        temperature=0.7
    )
    
    # Create a prompt template for Vietnamese content
    prompt_template = PromptTemplate(
        input_variables=["team","team1"],
        template="Viết một bài khen đội {team} và chửi {team1}"
    )
    
    chain = prompt_template | llm
    
    teams = ["MU","Barca","Liverpool"]

    print("🚀 LangChain examples")
    print("=" * 50)
    
    for team in teams:
        try:
            result = chain.invoke({"team": team,"team1": team})
            print(f"📝 Chủ đề: {team.title()}")
            print(f"✨ Thông tin: {result.content.strip()}")
            print("-" * 50)
        except Exception as e:
            print(f"❌ Lỗi khi xử lý {team}: {str(e)}")
            print("-" * 50)

if __name__ == "__main__":
    main()