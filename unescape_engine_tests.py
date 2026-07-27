import sys
import glob

for filename in glob.glob("app/src/test/java/com/example/**/*.kt", recursive=True):
    with open(filename, "r") as f:
        content = f.read()

    new_content = content.replace("\\`", "`")

    if new_content != content:
        with open(filename, "w") as f:
            f.write(new_content)

