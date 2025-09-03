#!/usr/bin/env python3
"""
Main Runner Script for HBase Spam Detection System.
This script runs the data generator first, then the spam detector.
"""

import subprocess
import sys
import time
from pathlib import Path

def run_command(command, description):
    """Run a command and handle errors."""
    print(f"🚀 {description}")
    print(f"   Command: {' '.join(command)}")
    print("-" * 50)
    
    try:
        # Run the command
        result = subprocess.run(
            command,
            check=True,
            capture_output=False,  # Show output in real-time
            text=True
        )
        
        print(f"✅ {description} completed successfully")
        return True
        
    except subprocess.CalledProcessError as e:
        print(f"❌ {description} failed with exit code {e.returncode}")
        return False
    except FileNotFoundError:
        print(f"❌ Command not found: {command[0]}")
        return False
    except Exception as e:
        print(f"❌ Unexpected error in {description}: {str(e)}")
        return False

def check_dependencies():
    """Check if required dependencies are available."""
    print("🔍 Checking dependencies...")
    
    # Check Python
    try:
        python_version = subprocess.run([sys.executable, "--version"], 
                                      capture_output=True, text=True, check=True)
        print(f"   ✅ Python: {python_version.stdout.strip()}")
    except:
        print("   ❌ Python not found")
        return False
    
    # Check if files exist
    current_dir = Path(__file__).parent
    generator_file = current_dir / "hbase_data_generator.py"
    detector_file = current_dir / "spam_detector_hbase.py"
    
    if not generator_file.exists():
        print(f"   ❌ Generator file not found: {generator_file}")
        return False
    else:
        print(f"   ✅ Generator file: {generator_file}")
    
    if not detector_file.exists():
        print(f"   ❌ Detector file not found: {detector_file}")
        return False
    else:
        print(f"   ✅ Detector file: {detector_file}")
    
    # Check .env file
    env_file = current_dir / ".env"
    if not env_file.exists():
        print(f"   ⚠️  .env file not found: {env_file}")
        print("   Make sure GROQ_API_KEY is available in environment")
    else:
        print(f"   ✅ .env file: {env_file}")
    
    return True

def main():
    """Main function to run the complete spam detection workflow."""
    print("🛡️  HBase Spam Detection System Runner")
    print("=" * 60)
    
    # Check dependencies
    if not check_dependencies():
        print("\n❌ Dependency check failed. Please fix the issues above.")
        sys.exit(1)
    
    print("\n🎯 Starting complete spam detection workflow...")
    print("=" * 60)
    
    current_dir = Path(__file__).parent
    
    # Step 1: Run data generator
    print("\n📝 Step 1: Generating and inserting data into HBase")
    generator_cmd = [sys.executable, str(current_dir / "hbase_data_generator.py")]
    
    if not run_command(generator_cmd, "Data generation"):
        print("\n❌ Data generation failed. Cannot proceed with detection.")
        sys.exit(1)
    
    # Wait a bit to ensure data is properly inserted
    print("\n⏳ Waiting 3 seconds for data to settle...")
    time.sleep(3)
    
    # Step 2: Run spam detector
    print("\n🔍 Step 2: Running spam detection analysis")
    detector_cmd = [sys.executable, str(current_dir / "spam_detector_hbase.py")]
    
    if not run_command(detector_cmd, "Spam detection"):
        print("\n❌ Spam detection failed.")
        sys.exit(1)
    
    print("\n🎉 Complete workflow finished successfully!")
    print("=" * 60)
    print("📊 Summary:")
    print("   1. ✅ Data generated and inserted into HBase")
    print("   2. ✅ Spam detection analysis completed")
    print("   3. ✅ Results displayed with risk assessment")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n⛔ Workflow interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {str(e)}")
        sys.exit(1)