#!/bin/bash
if [ -f app/build/reports/tests/testDebugUnitTest/index.html ]; then
    echo "Found test results:"
    grep -A 2 -B 2 "failures" app/build/reports/tests/testDebugUnitTest/index.html
else
    echo "No test results found"
fi
