#!/usr/bin/env python3
"""
Test HBase connection script.
"""

import happybase
import time

def test_connection():
    """Test HBase connection with different configurations."""
    
    configs = [
        {"host": "localhost", "port": 2182},
        {"host": "hbase", "port": 2182}, 
        {"host": "localhost", "port": 9090},
        {"host": "127.0.0.1", "port": 2182},
    ]
    
    for config in configs:
        host = config["host"]
        port = config["port"]
        
        print(f"🔌 Testing connection to {host}:{port}")
        
        try:
            connection = happybase.Connection(
                host=host,
                port=port,
                timeout=5000  # 5 second timeout
            )
            
            # Test basic operation
            tables = connection.tables()
            print(f"✅ Success! Found {len(tables)} tables: {tables}")
            
            connection.close()
            return True
            
        except Exception as e:
            print(f"❌ Failed: {str(e)}")
            continue
    
    print("🚫 All connection attempts failed")
    return False

def check_ports():
    """Check if HBase ports are accessible."""
    import socket
    
    ports_to_check = [2181, 2182, 9090, 16000, 16010]
    hosts_to_check = ["localhost", "127.0.0.1"]
    
    print("🔍 Checking port accessibility...")
    
    for host in hosts_to_check:
        for port in ports_to_check:
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(2)
                result = sock.connect_ex((host, port))
                sock.close()
                
                if result == 0:
                    print(f"✅ {host}:{port} is accessible")
                else:
                    print(f"❌ {host}:{port} is not accessible")
            except Exception as e:
                print(f"❌ {host}:{port} - Error: {str(e)}")

if __name__ == "__main__":
    print("🛠️  HBase Connection Test")
    print("=" * 40)
    
    # Check ports first
    check_ports()
    print()
    
    # Test connections
    test_connection()