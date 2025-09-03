#!/usr/bin/env python3
"""
HBase-enabled Spam Detection RAG System using LangChain and Groq DeepSeek.
This system reads user messages from HBase and analyzes them to identify potential spammers.
"""

import os
import json
from typing import List, Dict, Any, Optional
from datetime import datetime
from dataclasses import dataclass
from dotenv import load_dotenv
import happybase

# Fix HuggingFace tokenizers warning
os.environ["TOKENIZERS_PARALLELISM"] = "false"

from langchain.chains import RetrievalQA
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_groq import ChatGroq
from langchain_huggingface import HuggingFaceEmbeddings
from langchain.schema import Document
from langchain.prompts import PromptTemplate

@dataclass
class UserMessage:
    """Represents a user message."""
    user_id: str
    message_id: str
    content: str
    timestamp: datetime
    metadata: Optional[Dict[str, Any]] = None

@dataclass
class SpamAnalysisResult:
    """Result of spam analysis for a user."""
    user_id: str
    spam_score: float  # 0.0 to 1.0
    risk_level: str   # "low", "medium", "high"
    reasons: List[str]
    message_count: int
    confidence: float

class HBaseSpamDetectorRAG:
    """HBase-enabled RAG-based spam detection system."""
    
    def __init__(self, groq_api_key: str = None, hbase_host='localhost', hbase_port=9090, namespace='hulk'):
        """Initialize the HBase spam detection RAG system."""
        load_dotenv()
        
        self.groq_api_key = groq_api_key or os.getenv("GROQ_API_KEY")
        if not self.groq_api_key:
            raise ValueError("GROQ_API_KEY is required")
        
        # HBase configuration
        self.hbase_host = hbase_host
        self.hbase_port = hbase_port
        self.namespace = namespace
        self.table_name = 'user_messages'
        self.hbase_connection = None
        
        # Initialize embeddings and LLM
        self.embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
        self.llm = ChatGroq(
            groq_api_key=self.groq_api_key,
            model_name="deepseek-r1-distill-llama-70b",
            temperature=0.1  # Low temperature for consistent analysis
        )
        
        # Initialize knowledge base
        self.vectorstore = None
        self.qa_chain = None
        self._setup_spam_knowledge_base()
    
    def connect_hbase(self):
        """Connect to HBase."""
        try:
            print(f"🔌 Connecting to HBase at {self.hbase_host}:{self.hbase_port}")
            self.hbase_connection = happybase.Connection(
                host=self.hbase_host,
                port=self.hbase_port,
                timeout=30000
            )
            print("✅ Connected to HBase successfully")
            return True
        except Exception as e:
            print(f"❌ Failed to connect to HBase: {str(e)}")
            return False
    
    def get_quota_info(self):
        """Get current Groq quota information."""
        try:
            import requests
            
            headers = {
                "Authorization": f"Bearer {self.groq_api_key}",
                "Content-Type": "application/json"
            }
            
            # Try to make a simple request to get quota info
            response = requests.get(
                "https://api.groq.com/openai/v1/models",
                headers=headers
            )
            
            if response.status_code == 200:
                # Check response headers for rate limit info
                quota_info = {}
                if 'x-ratelimit-limit-requests' in response.headers:
                    quota_info['requests_limit'] = response.headers['x-ratelimit-limit-requests']
                if 'x-ratelimit-remaining-requests' in response.headers:
                    quota_info['requests_remaining'] = response.headers['x-ratelimit-remaining-requests']
                if 'x-ratelimit-limit-tokens' in response.headers:
                    quota_info['tokens_limit'] = response.headers['x-ratelimit-limit-tokens']
                if 'x-ratelimit-remaining-tokens' in response.headers:
                    quota_info['tokens_remaining'] = response.headers['x-ratelimit-remaining-tokens']
                
                return quota_info
            else:
                return {"error": f"API request failed with status {response.status_code}"}
                
        except Exception as e:
            return {"error": f"Failed to get quota info: {str(e)}"}
        
    def _setup_spam_knowledge_base(self):
        """Create a knowledge base of spam patterns and indicators."""
        spam_knowledge = [
            {
                "content": """
                Common spam patterns include:
                - Repetitive messages with identical or near-identical content
                - Excessive use of promotional language and offers
                - Messages containing multiple links or suspicious URLs
                - Bulk messaging to many recipients simultaneously
                - Messages with poor grammar and spelling errors
                - Excessive use of capital letters and exclamation marks
                - Generic greetings and impersonal content
                """,
                "metadata": {"category": "spam_patterns", "source": "general_indicators"}
            },
            {
                "content": """
                Behavioral indicators of spam accounts:
                - High message frequency in short time periods
                - Similar message templates across different conversations
                - Lack of personalized responses to specific questions
                - Profile information that is incomplete or generic
                - Messages that don't respond appropriately to context
                - Automated or bot-like response patterns
                """,
                "metadata": {"category": "behavioral_patterns", "source": "user_behavior"}
            },
            {
                "content": """
                Content analysis for spam detection:
                - Commercial promotion and sales language
                - Requests for personal information or financial details
                - Links to external websites or download requests
                - Messages encouraging urgent action or time-limited offers
                - Repetitive keywords and phrases
                - Messages that seem unrelated to the conversation context
                """,
                "metadata": {"category": "content_analysis", "source": "message_content"}
            },
            {
                "content": """
                Legitimate user communication characteristics:
                - Natural conversational flow and context awareness
                - Personalized responses to specific questions
                - Varied vocabulary and sentence structures
                - Appropriate response timing and frequency
                - Relevant and contextual message content
                - Proper grammar and spelling in most cases
                """,
                "metadata": {"category": "legitimate_patterns", "source": "normal_behavior"}
            }
        ]
        
        # Create documents from knowledge base
        documents = []
        for knowledge in spam_knowledge:
            documents.append(Document(
                page_content=knowledge["content"].strip(),
                metadata=knowledge["metadata"]
            ))
        
        # Split documents for better retrieval
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=300,
            chunk_overlap=50
        )
        texts = text_splitter.split_documents(documents)
        
        # Create vector store
        self.vectorstore = FAISS.from_documents(texts, self.embeddings)
        
        # Create QA chain with custom prompt
        prompt_template = """You are an expert spam detection analyst. Use the following context about spam patterns and user behavior to analyze if the provided user messages indicate spam behavior.

IMPORTANT: Be balanced in your analysis. Most normal users should score LOW to MEDIUM risk. Only clearly malicious patterns should be HIGH risk.

Scoring Guidelines:
- LOW (0.0-0.3): Normal conversation, personalized messages, reasonable frequency
- MEDIUM (0.3-0.7): Some suspicious elements but could be legitimate 
- HIGH (0.7-1.0): Clear spam patterns, promotional content, suspicious links, very high frequency

Context: {context}

User Messages Analysis Request: {question}

Provide a detailed analysis including:
1. Spam probability score (0.0 to 1.0) - be conservative, err on the side of legitimate users
2. Risk level (low/medium/high) 
3. Specific reasons for the assessment
4. Confidence level in the analysis

Answer:"""

        PROMPT = PromptTemplate(
            template=prompt_template, 
            input_variables=["context", "question"]
        )
        
        self.qa_chain = RetrievalQA.from_chain_type(
            llm=self.llm,
            chain_type="stuff",
            retriever=self.vectorstore.as_retriever(search_kwargs={"k": 3}),
            chain_type_kwargs={"prompt": PROMPT},
            return_source_documents=True
        )
    
    def load_messages_from_hbase(self) -> Dict[str, List[UserMessage]]:
        """Load user messages from HBase."""
        try:
            full_table_name = f"{self.namespace}:{self.table_name}"
            table = self.hbase_connection.table(full_table_name)
            
            print(f"📖 Loading messages from HBase table: {full_table_name}")
            
            user_messages = {}
            message_count = 0
            
            for key, data in table.scan():
                try:
                    # Parse data
                    user_id = data[b'msg:user_id'].decode('utf-8')
                    message_id = data[b'msg:message_id'].decode('utf-8')
                    content = data[b'msg:content'].decode('utf-8')
                    timestamp_str = data[b'msg:timestamp'].decode('utf-8')
                    timestamp = datetime.fromisoformat(timestamp_str)
                    
                    # Parse metadata if exists
                    metadata = None
                    if b'meta:additional' in data:
                        metadata = json.loads(data[b'meta:additional'].decode('utf-8'))
                    
                    # Create UserMessage object
                    message = UserMessage(
                        user_id=user_id,
                        message_id=message_id,
                        content=content,
                        timestamp=timestamp,
                        metadata=metadata
                    )
                    
                    # Group by user
                    if user_id not in user_messages:
                        user_messages[user_id] = []
                    user_messages[user_id].append(message)
                    message_count += 1
                    
                except Exception as e:
                    print(f"⚠️  Error parsing message: {str(e)}")
                    continue
            
            print(f"✅ Loaded {message_count} messages from {len(user_messages)} users")
            
            # Sort messages by timestamp for each user
            for user_id in user_messages:
                user_messages[user_id].sort(key=lambda x: x.timestamp)
            
            return user_messages
            
        except Exception as e:
            print(f"❌ Error loading messages from HBase: {str(e)}")
            return {}
    
    def preprocess_messages(self, user_messages: Dict[str, List[UserMessage]]) -> Dict[str, str]:
        """Preprocess user messages for analysis."""
        processed_data = {}
        
        for user_id, messages in user_messages.items():
            # Sort messages by timestamp
            sorted_messages = sorted(messages, key=lambda x: x.timestamp)
            
            # Create analysis summary
            message_contents = [msg.content for msg in sorted_messages]
            timestamps = [msg.timestamp.isoformat() for msg in sorted_messages]
            
            analysis_text = f"""
            User ID: {user_id}
            Total Messages: {len(messages)}
            Message Frequency: {len(messages)} messages
            Time Range: {timestamps[0]} to {timestamps[-1]}
            
            Message Contents:
            {chr(10).join([f"{i+1}. {content}" for i, content in enumerate(message_contents)])}
            
            Message Patterns Analysis:
            - Average message length: {sum(len(msg) for msg in message_contents) / len(message_contents):.1f} characters
            - Unique message ratio: {len(set(message_contents)) / len(message_contents):.2f}
            - Time gaps between messages: varied
            """
            
            processed_data[user_id] = analysis_text
        
        return processed_data
    
    def analyze_user_spam_risk(self, user_id: str, analysis_text: str) -> SpamAnalysisResult:
        """Analyze spam risk for a single user."""
        query = f"""
        Analyze the following user's message patterns for spam indicators:
        
        {analysis_text}
        
        Please provide:
        1. A spam probability score from 0.0 (definitely not spam) to 1.0 (definitely spam)
        2. Risk level classification (low: 0.0-0.3, medium: 0.3-0.7, high: 0.7-1.0)
        3. Specific reasons for this assessment
        4. Confidence level in this analysis
        """
        
        try:
            print(f"   📝 Analyzing: {user_id}")
            result = self.qa_chain.invoke({"query": query})
            analysis = result['result']
            
            # Parse the LLM response
            spam_score = self._extract_score(analysis)
            risk_level = self._determine_risk_level(spam_score)
            reasons = self._extract_reasons(analysis)
            confidence = self._extract_confidence(analysis)
            
            # Count messages from analysis text
            message_count = self._extract_message_count(analysis_text)
            
            return SpamAnalysisResult(
                user_id=user_id,
                spam_score=spam_score,
                risk_level=risk_level,
                reasons=reasons,
                message_count=message_count,
                confidence=confidence
            )
            
        except Exception as e:
            print(f"Error analyzing user {user_id}: {str(e)}")
            return SpamAnalysisResult(
                user_id=user_id,
                spam_score=0.0,
                risk_level="low",
                reasons=["Analysis failed"],
                message_count=0,
                confidence=0.0
            )
    
    def batch_analyze_users(self, user_messages: Dict[str, List[UserMessage]]) -> List[SpamAnalysisResult]:
        """Analyze multiple users for spam risk."""
        print(f"🔍 Analyzing {len(user_messages)} users for spam patterns...")
        
        # Preprocess messages
        processed_data = self.preprocess_messages(user_messages)
        
        results = []
        for i, (user_id, analysis_text) in enumerate(processed_data.items(), 1):
            print(f"🔍 [{i}/{len(processed_data)}] Processing user: {user_id}")
            result = self.analyze_user_spam_risk(user_id, analysis_text)
            print(f"   ✅ Score: {result.spam_score:.2f} | Risk: {result.risk_level.upper()}")
            results.append(result)
            print()
        
        # Sort by spam score (highest risk first)
        results.sort(key=lambda x: x.spam_score, reverse=True)
        return results
    
    def _extract_score(self, analysis: str) -> float:
        """Extract spam score from LLM analysis."""
        try:
            import re
            # Look for patterns like "0.8", "0.3", etc.
            score_patterns = [
                r'spam probability score[:\s]*([0-9]+\.?[0-9]*)',
                r'probability score[:\s]*([0-9]+\.?[0-9]*)', 
                r'score[:\s]*([0-9]+\.?[0-9]*)',
                r'([0-9]+\.?[0-9]*)\s*(?:out of|/)\s*1\.?0?'
            ]
            
            for pattern in score_patterns:
                matches = re.findall(pattern, analysis.lower())
                if matches:
                    score = float(matches[0])
                    return min(1.0, max(0.0, score))
        except:
            pass
        
        # More nuanced keyword-based scoring
        high_spam_keywords = ['clear spam', 'definitely spam', 'obvious spam', 'malicious']
        medium_spam_keywords = ['suspicious', 'promotional', 'repetitive', 'concerning']
        low_spam_keywords = ['legitimate', 'normal', 'natural', 'genuine', 'conversational']
        
        analysis_lower = analysis.lower()
        
        high_count = sum(1 for keyword in high_spam_keywords if keyword in analysis_lower)
        medium_count = sum(1 for keyword in medium_spam_keywords if keyword in analysis_lower)  
        low_count = sum(1 for keyword in low_spam_keywords if keyword in analysis_lower)
        
        if high_count > 0:
            return 0.8
        elif medium_count > low_count:
            return 0.5
        elif low_count > 0:
            return 0.2
        else:
            return 0.4  # Default to lower score when uncertain
    
    def _determine_risk_level(self, score: float) -> str:
        """Determine risk level from spam score."""
        if score >= 0.7:
            return "high"
        elif score >= 0.3:
            return "medium"
        else:
            return "low"
    
    def _extract_reasons(self, analysis: str) -> List[str]:
        """Extract reasons from LLM analysis."""
        reasons = []
        lines = analysis.split('\n')
        
        # Look for numbered points or bullet points
        for line in lines:
            line = line.strip()
            if any(indicator in line.lower() for indicator in ['reason', 'because', 'due to', 'indicator']):
                if len(line) > 10:  # Avoid very short lines
                    reasons.append(line)
        
        if not reasons:
            reasons = ["Based on message pattern analysis"]
        
        return reasons[:5]  # Limit to top 5 reasons
    
    def _extract_confidence(self, analysis: str) -> float:
        """Extract confidence level from analysis."""
        try:
            import re
            confidence_pattern = r'confidence[:\s]*([0-9]+\.?[0-9]*)'
            matches = re.findall(confidence_pattern, analysis.lower())
            if matches:
                return min(1.0, max(0.0, float(matches[0])))
        except:
            pass
        return 0.8  # Default confidence
    
    def _extract_message_count(self, analysis_text: str) -> int:
        """Extract message count from analysis text."""
        try:
            import re
            count_pattern = r'Total Messages:\s*(\d+)'
            matches = re.findall(count_pattern, analysis_text)
            if matches:
                return int(matches[0])
        except:
            pass
        return 0
    
    def close(self):
        """Close HBase connection."""
        if self.hbase_connection:
            self.hbase_connection.close()
            print("🔌 HBase connection closed")

def main():
    """Main function to demonstrate spam detection from HBase."""
    print("🛡️  HBase Spam Detection RAG System")
    print("=" * 50)
    
    detector = None
    
    try:
        # Initialize spam detector
        detector = HBaseSpamDetectorRAG()
        
        # Connect to HBase
        if not detector.connect_hbase():
            return
        
        # Check quota info
        print("🔍 Checking Groq API quota...")
        quota_info = detector.get_quota_info()
        
        if 'error' not in quota_info:
            print("📊 Groq API Quota Information:")
            if 'requests_remaining' in quota_info:
                print(f"   🔄 Requests Remaining: {quota_info['requests_remaining']}")
            if 'requests_limit' in quota_info:
                print(f"   📈 Requests Limit: {quota_info['requests_limit']}")
            if 'tokens_remaining' in quota_info:
                print(f"   🎯 Tokens Remaining: {quota_info['tokens_remaining']}")
            if 'tokens_limit' in quota_info:
                print(f"   📊 Tokens Limit: {quota_info['tokens_limit']}")
            print()
        else:
            print(f"⚠️  Could not get quota info: {quota_info['error']}")
            print()
        
        # Load messages from HBase
        user_messages = detector.load_messages_from_hbase()
        
        if not user_messages:
            print("❌ No messages found in HBase. Please run the data generator first.")
            return
        
        # Analyze users
        results = detector.batch_analyze_users(user_messages)
        
        # Display results
        print("\n📋 Spam Analysis Results:")
        print("=" * 70)
        
        for result in results:
            print(f"\n👤 User: {result.user_id}")
            print(f"   📊 Spam Score: {result.spam_score:.2f}")
            print(f"   ⚠️  Risk Level: {result.risk_level.upper()}")
            print(f"   💬 Messages: {result.message_count}")
            print(f"   🎯 Confidence: {result.confidence:.2f}")
            print(f"   📝 Reasons:")
            for reason in result.reasons:
                print(f"      • {reason}")
            print("-" * 50)
        
        # Summary statistics
        high_risk = [r for r in results if r.risk_level == "high"]
        medium_risk = [r for r in results if r.risk_level == "medium"]
        low_risk = [r for r in results if r.risk_level == "low"]
        
        print(f"\n📈 Summary:")
        print(f"   🔴 High Risk: {len(high_risk)} users")
        print(f"   🟡 Medium Risk: {len(medium_risk)} users")
        print(f"   🟢 Low Risk: {len(low_risk)} users")
        
        if high_risk:
            print(f"\n⚠️  High Risk Users: {', '.join([r.user_id for r in high_risk])}")
            
    except KeyboardInterrupt:
        print("\n⛔ Operation cancelled by user")
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        print("Make sure GROQ_API_KEY is set in your .env file and HBase is running.")
    finally:
        if detector:
            detector.close()

if __name__ == "__main__":
    main()