import re

# Read the file that we broke. Wait, do we have the broken file?
# We need the file WITHOUT the wrong fixes we made.
# Wait, we don't have the original broken file anymore, because we overwrote it.
# BUT we can just STRIP all closing braces that have >= 4 spaces to recreate the broken state!
