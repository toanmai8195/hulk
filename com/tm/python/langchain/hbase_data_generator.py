#!/usr/bin/env python3
"""
HBase Data Generator for Spam Detection System.
This script generates and inserts user message data into HBase for spam analysis.
"""

import os
import json
from datetime import datetime, timedelta
from typing import List, Dict, Any
import happybase
from dataclasses import dataclass, asdict

@dataclass
class UserMessage:
    """Represents a user message."""
    user_id: str
    message_id: str
    content: str
    timestamp: datetime
    metadata: Dict[str, Any] = None

class HBaseDataGenerator:
    """Generator for creating and storing user message data in HBase."""
    
    def __init__(self, host='hbase', port=9090, namespace='hulk'):
        """Initialize HBase connection."""
        self.host = host
        self.port = port
        self.namespace = namespace
        self.connection = None
        self.table_name = 'user_messages'
        
    def connect(self):
        """Connect to HBase."""
        try:
            print(f"🔌 Connecting to HBase at {self.host}:{self.port}")
            self.connection = happybase.Connection(
                host=self.host,
                port=self.port,
                timeout=30000
            )
            print("✅ Connected to HBase successfully")
            return True
        except Exception as e:
            print(f"❌ Failed to connect to HBase: {str(e)}")
            return False
    
    def create_table_if_not_exists(self):
        """Create user_messages table if it doesn't exist."""
        try:
            full_table_name = f"{self.namespace}:{self.table_name}"
            
            # Check if table exists
            tables = self.connection.tables()
            table_exists = full_table_name.encode() in tables
            
            if not table_exists:
                print(f"📋 Creating table: {full_table_name}")
                self.connection.create_table(
                    full_table_name,
                    {
                        'msg': dict(),  # message content family
                        'meta': dict()  # metadata family
                    }
                )
                print("✅ Table created successfully")
            else:
                print(f"📋 Table {full_table_name} already exists")
                
            return True
        except Exception as e:
            print(f"❌ Error creating table: {str(e)}")
            return False
    
    def generate_sample_messages(self) -> Dict[str, List[UserMessage]]:
        """Generate sample user messages for testing."""
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
            "tech_user": [
                UserMessage("tech_user", "msg001", "Having issues with the database connection. Can you help?", base_time),
                UserMessage("tech_user", "msg002", "The error occurs when trying to connect to the production server.", base_time + timedelta(minutes=30)),
                UserMessage("tech_user", "msg003", "Fixed it! The issue was with the connection pool settings.", base_time + timedelta(hours=1)),
                UserMessage("tech_user", "msg004", "Thanks for the debugging tips, they were very helpful.", base_time + timedelta(hours=2)),
            ]
        }
        
        return users_data
    
    def insert_messages_to_hbase(self, user_messages: Dict[str, List[UserMessage]]):
        """Insert user messages into HBase."""
        try:
            full_table_name = f"{self.namespace}:{self.table_name}"
            table = self.connection.table(full_table_name)
            
            print(f"📝 Inserting messages into {full_table_name}")
            
            total_messages = sum(len(messages) for messages in user_messages.values())
            current_count = 0
            
            for user_id, messages in user_messages.items():
                print(f"   👤 Processing user: {user_id} ({len(messages)} messages)")
                
                for msg in messages:
                    current_count += 1
                    
                    # Create row key: user_id + timestamp + message_id
                    timestamp_str = msg.timestamp.strftime("%Y%m%d%H%M%S")
                    row_key = f"{user_id}_{timestamp_str}_{msg.message_id}"
                    
                    # Prepare data
                    data = {
                        b'msg:content': msg.content.encode('utf-8'),
                        b'msg:user_id': msg.user_id.encode('utf-8'),
                        b'msg:message_id': msg.message_id.encode('utf-8'),
                        b'msg:timestamp': msg.timestamp.isoformat().encode('utf-8'),
                        b'meta:created_at': datetime.now().isoformat().encode('utf-8'),
                        b'meta:source': b'spam_detector_system'
                    }
                    
                    # Add metadata if exists
                    if msg.metadata:
                        data[b'meta:additional'] = json.dumps(msg.metadata).encode('utf-8')
                    
                    # Insert into HBase
                    table.put(row_key.encode('utf-8'), data)
                    
                    if current_count % 5 == 0:
                        print(f"   📊 Progress: {current_count}/{total_messages} messages inserted")
            
            print(f"✅ Successfully inserted {total_messages} messages into HBase")
            return True
            
        except Exception as e:
            print(f"❌ Error inserting messages: {str(e)}")
            return False
    
    def verify_data(self):
        """Verify that data was inserted correctly."""
        try:
            full_table_name = f"{self.namespace}:{self.table_name}"
            table = self.connection.table(full_table_name)
            
            print(f"🔍 Verifying data in {full_table_name}")
            
            # Scan table to count rows
            count = 0
            users = set()
            
            for key, data in table.scan():
                count += 1
                user_id = data[b'msg:user_id'].decode('utf-8')
                users.add(user_id)
            
            print(f"📊 Verification results:")
            print(f"   📝 Total messages: {count}")
            print(f"   👥 Unique users: {len(users)}")
            print(f"   📋 Users: {', '.join(sorted(users))}")
            
            return count > 0
            
        except Exception as e:
            print(f"❌ Error verifying data: {str(e)}")
            return False
    
    def clear_table(self):
        """Clear all data from the table (for testing purposes)."""
        try:
            full_table_name = f"{self.namespace}:{self.table_name}"
            table = self.connection.table(full_table_name)
            
            print(f"🗑️  Clearing table: {full_table_name}")
            
            # Get all row keys
            row_keys = []
            for key, data in table.scan():
                row_keys.append(key)
            
            # Delete all rows
            for key in row_keys:
                table.delete(key)
            
            print(f"✅ Cleared {len(row_keys)} rows from table")
            return True
            
        except Exception as e:
            print(f"❌ Error clearing table: {str(e)}")
            return False
    
    def close(self):
        """Close HBase connection."""
        if self.connection:
            self.connection.close()
            print("🔌 HBase connection closed")

def main():
    """Main function to generate and insert data."""
    print("🗄️  HBase Data Generator for Spam Detection")
    print("=" * 50)
    
    # Initialize generator
    generator = HBaseDataGenerator()
    
    try:
        # Connect to HBase
        if not generator.connect():
            return
        
        # Create table
        if not generator.create_table_if_not_exists():
            return
        
        # Clear existing data (optional)
        clear_data = input("🗑️  Clear existing data? (y/n): ").strip().lower()
        if clear_data == 'y':
            generator.clear_table()
        
        # Generate sample data
        print("🎲 Generating sample user messages...")
        user_messages = generator.generate_sample_messages()
        
        total_users = len(user_messages)
        total_messages = sum(len(messages) for messages in user_messages.values())
        print(f"📊 Generated {total_messages} messages from {total_users} users")
        
        # Insert data into HBase
        if generator.insert_messages_to_hbase(user_messages):
            # Verify insertion
            generator.verify_data()
            print("\n🎉 Data generation completed successfully!")
        else:
            print("\n❌ Data generation failed!")
            
    except KeyboardInterrupt:
        print("\n⛔ Operation cancelled by user")
    except Exception as e:
        print(f"\n❌ Unexpected error: {str(e)}")
    finally:
        generator.close()

if __name__ == "__main__":
    main()