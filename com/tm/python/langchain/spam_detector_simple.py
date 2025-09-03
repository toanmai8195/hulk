#!/usr/bin/env python3
"""
Simple Spam Detection System using LangChain and Groq DeepSeek (No RAG).
This system analyzes user messages to identify potential spammers using direct LLM analysis.
"""

import os
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
from dataclasses import dataclass
from dotenv import load_dotenv

# Fix HuggingFace tokenizers warning
os.environ["TOKENIZERS_PARALLELISM"] = "false"

from langchain_groq import ChatGroq
from langchain.prompts import PromptTemplate
from langchain.schema import BaseOutputParser

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

class SpamAnalysisParser(BaseOutputParser):
    """Parser for spam analysis results from LLM."""
    
    def parse(self, text: str) -> Dict[str, Any]:
        """Parse LLM output into structured data."""
        import re
        
        # Extract spam score
        score_pattern = r'(?:spam.{0,20}score|probability|risk).{0,20}([0-9]*\.?[0-9]+)'
        score_matches = re.findall(score_pattern, text.lower())
        spam_score = 0.5  # default
        if score_matches:
            try:
                spam_score = min(1.0, max(0.0, float(score_matches[0])))
            except:
                pass
        
        # Extract risk level
        risk_level = "medium"  # default
        if any(word in text.lower() for word in ['low risk', 'legitimate', 'normal']):
            risk_level = "low"
        elif any(word in text.lower() for word in ['high risk', 'spam', 'suspicious']):
            risk_level = "high"
        
        # Extract confidence
        conf_pattern = r'confidence.{0,20}([0-9]*\.?[0-9]+)'
        conf_matches = re.findall(conf_pattern, text.lower())
        confidence = 0.8  # default
        if conf_matches:
            try:
                confidence = min(1.0, max(0.0, float(conf_matches[0])))
            except:
                pass
        
        # Extract reasons (simple approach)
        lines = text.split('\n')
        reasons = []
        for line in lines:
            line = line.strip()
            if len(line) > 20 and any(word in line.lower() for word in 
                                     ['because', 'due to', 'reason', 'indicator', 'shows', 'exhibits']):
                reasons.append(line[:100])  # Limit length
        
        if not reasons:
            reasons = ["Based on message content and pattern analysis"]
        
        return {
            'spam_score': spam_score,
            'risk_level': risk_level,
            'confidence': confidence,
            'reasons': reasons[:3]  # Top 3 reasons
        }

class SimpleSpamDetector:
    """Simple spam detection system without RAG."""
    
    def __init__(self, groq_api_key: str = None):
        """Initialize the spam detection system."""
        load_dotenv()
        
        self.groq_api_key = groq_api_key or os.getenv("GROQ_API_KEY")
        if not self.groq_api_key:
            raise ValueError("GROQ_API_KEY is required")
        
        # Initialize LLM
        self.llm = ChatGroq(
            groq_api_key=self.groq_api_key,
            model_name="deepseek-r1-distill-llama-70b",
            temperature=0.1
        )
        
        # Initialize parser
        self.parser = SpamAnalysisParser()
        
        # Create prompt template
        self.prompt_template = PromptTemplate(
            input_variables=["user_analysis"],
            template="""You are an expert spam detection analyst. Analyze the following user's message patterns to determine if they indicate spam behavior.

IMPORTANT: Be balanced in your analysis. Most normal users should score LOW risk. Only clearly malicious patterns should be HIGH risk.

Scoring Guidelines:
- LOW (0.0-0.3): Normal conversation, personalized messages, reasonable frequency
- MEDIUM (0.3-0.6): Some suspicious elements but could be legitimate 
- HIGH (0.7-1.0): Clear spam patterns, promotional content, suspicious links, very high frequency

User Analysis:
{user_analysis}

Please analyze and provide:
1. Spam probability score (0.0 to 1.0) - be conservative, err on the side of legitimate users
2. Risk level (low/medium/high)
3. Specific reasons for your assessment (2-3 main points)
4. Confidence level in your analysis (0.0 to 1.0)

Format your response clearly with these sections."""
        )
    
    def get_quota_info(self):
        """Get current Groq quota information."""
        try:
            import requests
            
            headers = {
                "Authorization": f"Bearer {self.groq_api_key}",
                "Content-Type": "application/json"
            }
            
            response = requests.get(
                "https://api.groq.com/openai/v1/models",
                headers=headers
            )
            
            if response.status_code == 200:
                quota_info = {}
                headers_to_check = [
                    'x-ratelimit-limit-requests',
                    'x-ratelimit-remaining-requests', 
                    'x-ratelimit-limit-tokens',
                    'x-ratelimit-remaining-tokens'
                ]
                
                for header in headers_to_check:
                    if header in response.headers:
                        quota_info[header.replace('x-ratelimit-', '').replace('-', '_')] = response.headers[header]
                
                return quota_info
            else:
                return {"error": f"API request failed with status {response.status_code}"}
                
        except Exception as e:
            return {"error": f"Failed to get quota info: {str(e)}"}
    
    def preprocess_messages(self, user_messages: Dict[str, List[UserMessage]]) -> Dict[str, str]:
        """Preprocess user messages for analysis."""
        processed_data = {}
        
        for user_id, messages in user_messages.items():
            # Sort messages by timestamp
            sorted_messages = sorted(messages, key=lambda x: x.timestamp)
            
            # Create analysis summary
            message_contents = [msg.content for msg in sorted_messages]
            timestamps = [msg.timestamp.isoformat() for msg in sorted_messages]
            
            # Calculate timing patterns
            time_gaps = []
            if len(sorted_messages) > 1:
                for i in range(1, len(sorted_messages)):
                    gap = (sorted_messages[i].timestamp - sorted_messages[i-1].timestamp).total_seconds() / 60
                    time_gaps.append(gap)
            
            analysis_text = f"""
User ID: {user_id}
Total Messages: {len(messages)}
Time Range: {timestamps[0]} to {timestamps[-1]} ({len(messages)} messages)
Average time gap: {sum(time_gaps) / len(time_gaps):.1f} minutes (if applicable)

Messages:
{chr(10).join([f"{i+1}. [{sorted_messages[i].timestamp.strftime('%H:%M')}] {content}" 
              for i, content in enumerate(message_contents)])}

Analysis Metrics:
- Average message length: {sum(len(msg) for msg in message_contents) / len(message_contents):.1f} characters
- Unique messages ratio: {len(set(message_contents)) / len(message_contents):.2f}
- Contains links: {'Yes' if any('http' in msg or 'www.' in msg for msg in message_contents) else 'No'}
- Contains promotional words: {'Yes' if any(word in ' '.join(message_contents).lower() 
                                           for word in ['free', 'deal', 'offer', 'urgent', 'limited', 'click', 'download']) else 'No'}
- All caps usage: {'Yes' if any(msg.isupper() for msg in message_contents) else 'No'}
"""
            
            processed_data[user_id] = analysis_text.strip()
        
        return processed_data
    
    def analyze_user_spam_risk(self, user_id: str, analysis_text: str) -> SpamAnalysisResult:
        """Analyze spam risk for a single user."""
        try:
            print(f"   📝 Analyzing: {user_id}")
            
            # Create prompt
            prompt = self.prompt_template.format(user_analysis=analysis_text)
            
            # Get response from LLM
            response = self.llm.invoke(prompt)
            analysis = response.content
            
            # Parse the response
            parsed_result = self.parser.parse(analysis)
            
            # Extract message count from analysis text
            import re
            count_match = re.search(r'Total Messages:\s*(\d+)', analysis_text)
            message_count = int(count_match.group(1)) if count_match else 0
            
            return SpamAnalysisResult(
                user_id=user_id,
                spam_score=parsed_result['spam_score'],
                risk_level=parsed_result['risk_level'],
                reasons=parsed_result['reasons'],
                message_count=message_count,
                confidence=parsed_result['confidence']
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

def create_sample_user_messages() -> Dict[str, List[UserMessage]]:
    """Create sample user messages for testing."""
    base_time = datetime.now()
    
    users_data = {
        "clear_spammer": [
            UserMessage("clear_spammer", "msg001", "🎉 AMAZING DEAL! Get 90% OFF now! Limited time only!!! Click www.spam-site.com", base_time),
            UserMessage("clear_spammer", "msg002", "🔥 BEST PRICES! Click here for incredible savings! Don't miss out!!!", base_time + timedelta(minutes=2)),
            UserMessage("clear_spammer", "msg003", "💰 MONEY BACK GUARANTEE! Order now and save BIG! Special offer!!!", base_time + timedelta(minutes=3)),
            UserMessage("clear_spammer", "msg004", "⚡ URGENT! Last chance to get this deal! Act NOW!!!", base_time + timedelta(minutes=5)),
            UserMessage("clear_spammer", "msg005", "FREE GIFT CARDS! Download now! www.scam-site.com", base_time + timedelta(minutes=6)),
        ],
        "normal_user": [
            UserMessage("normal_user", "msg001", "Hi there! How are you doing today?", base_time),
            UserMessage("normal_user", "msg002", "I was wondering if you could help me with a question about the project we discussed yesterday.", base_time + timedelta(hours=1)),
            UserMessage("normal_user", "msg003", "Thanks for the information about the deadline. That makes sense now.", base_time + timedelta(hours=2)),
            UserMessage("normal_user", "msg004", "Just wanted to follow up on our conversation. Let me know when you're available to chat.", base_time + timedelta(days=1)),
        ],
        "casual_user": [
            UserMessage("casual_user", "msg001", "Hey! What's up?", base_time),
            UserMessage("casual_user", "msg002", "Did you see the game last night? Amazing!", base_time + timedelta(hours=3)),
            UserMessage("casual_user", "msg003", "Btw, thanks for recommending that restaurant. The food was great!", base_time + timedelta(days=1)),
        ],
        "suspicious_user": [
            UserMessage("suspicious_user", "msg001", "Hey! Check out this link: www.suspicious-site.com", base_time),
            UserMessage("suspicious_user", "msg002", "Free money! Click here to claim your reward!", base_time + timedelta(minutes=10)),
            UserMessage("suspicious_user", "msg003", "Download this file for amazing content!", base_time + timedelta(minutes=15)),
            UserMessage("suspicious_user", "msg004", "Join now and earn $1000 daily!", base_time + timedelta(minutes=20)),
            UserMessage("suspicious_user", "msg005", "Limited spots available! Register today!", base_time + timedelta(minutes=25)),
        ],
        "business_user": [
            UserMessage("business_user", "msg001", "Hello, I wanted to inquire about your services.", base_time),
            UserMessage("business_user", "msg002", "Could you please send me a quote for the project we discussed?", base_time + timedelta(hours=2)),
            UserMessage("business_user", "msg003", "Thank you for the detailed information. I'll review it with my team.", base_time + timedelta(hours=4)),
        ],
    }
    
    return users_data

def main():
    """Main function to demonstrate spam detection."""
    print("🛡️  Simple Spam Detection System (No RAG)")
    print("=" * 55)
    
    try:
        # Initialize spam detector
        detector = SimpleSpamDetector()
        
        # Check quota info
        print("🔍 Checking Groq API quota...")
        quota_info = detector.get_quota_info()
        
        if 'error' not in quota_info:
            print("📊 Groq API Quota Information:")
            for key, value in quota_info.items():
                if 'remaining' in key:
                    print(f"   🔄 {key.replace('_', ' ').title()}: {value}")
                elif 'limit' in key:
                    print(f"   📈 {key.replace('_', ' ').title()}: {value}")
            print()
        else:
            print(f"⚠️  Could not get quota info: {quota_info['error']}")
            print()
        
        # Create sample data
        user_messages = create_sample_user_messages()
        
        print(f"📊 Loaded messages from {len(user_messages)} users")
        
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
            
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        print("Make sure GROQ_API_KEY is set in your .env file.")

if __name__ == "__main__":
    main()