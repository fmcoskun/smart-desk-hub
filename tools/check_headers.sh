#!/bin/bash
# Lists Kotlin files whose first top-level declaration has no KDoc (/** ... */) right before it
# (only annotations may sit in between). Spec §8: every file starts with "what is this, which
# layer". Prints nothing and exits 0 when all files are documented; exits 1 otherwise.
#   tools/check_headers.sh            # checks app/src/main
#   tools/check_headers.sh some/dir   # checks another tree
cd "$(dirname "$0")/.."
missing=$(find "${1:-app/src/main}" -name '*.kt' | sort | while read -r f; do
  awk -v file="$f" '
    BEGIN { doc = 0; inDoc = 0 }
    /^@file:/                       { next }                   # file annotations are not declarations
    /^(package|import) /            { doc = 0; next }          # a KDoc above an import is not a header
    /^\/\*\*/                       { inDoc = 1 }
    inDoc && /\*\//                 { inDoc = 0; doc = 1; next }
    inDoc                           { next }
    /^[[:space:]]*$/                { next }
    /^\/\//                         { next }
    /^@/                            { next }                   # annotations between KDoc and declaration
    /^([[:space:]]|\))/             { next }                   # argument lines of a multi-line annotation
    /^(public |private |internal |protected )?((abstract|open|sealed|data|enum|annotation|inline|value|suspend|operator|infix|tailrec|external|const|fun) )*(class|object|interface|fun|val|var|typealias)[ <(]/ {
      if (!doc) print file
      done = 1; exit
    }
    { doc = 0 }                                                # anything else between KDoc and declaration
  ' "$f"
done)
if [ -n "$missing" ]; then
  echo "$missing"
  exit 1
fi
