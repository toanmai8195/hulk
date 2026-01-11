#!/bin/bash

# Script to reset HBase table (drop and recreate with proper regions)

echo "=================================================="
echo "  Reset HBase Table with Pre-Split Regions"
echo "=================================================="

TABLE="hulk:segment_index"

echo ""
echo "⚠️  WARNING: This will DELETE all data in table: $TABLE"
echo ""
read -p "Are you sure? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
  echo "Aborted."
  exit 0
fi

echo ""
echo "Connecting to HBase..."

# Execute HBase shell commands
docker exec -it hbase hbase shell <<EOF
disable '$TABLE'
drop '$TABLE'
exit
EOF

echo ""
echo "✅ Table dropped successfully"
echo ""
echo "Next steps:"
echo "  1. Run: ./com/tm/kotlin/service/hbase_bulkload/rebuild.sh 1"
echo "     This will create the table with 10 pre-split regions"
echo ""
