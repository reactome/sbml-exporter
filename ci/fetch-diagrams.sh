#!/usr/bin/env bash
# Fetches the diagram files a comparison or dry run needs (spec 001, T018; research R8)
# from the release's download directory, {base}/{release}/diagram/{stId}.json: one at a
# time, in sequence, with a pause between, into a folder outside this repository. A file
# already in the folder is not fetched again. An id with no file (a reaction, or a pathway
# drawn only on a parent's diagram) is listed and skipped; the download store answers 403 for
# a missing file as well as 404, so both count. A file missed this way is not lost silently:
# layout.DiagramFiles refuses a pathway the graph says has a diagram but whose file is missing.
# Any other failure stops.
# Usage: ci/fetch-diagrams.sh <release> <folder> <stId>...
#        ci/fetch-diagrams.sh <release> <folder> < ids.txt
set -euo pipefail
base=${DIAGRAM_BASE:-https://download.reactome.org}
pause=${DIAGRAM_PAUSE:-1}

if [[ $# -lt 2 || ! $1 =~ ^[0-9]+$ ]]; then
  sed -n '10,11s/^# //p' "$0" >&2
  exit 2
fi
release=$1 folder=$2
shift 2
if (( $# )); then ids=("$@"); else mapfile -t ids < <(grep -oE 'R-[A-Z]{3}-[0-9]+'); fi

folder=$(realpath -m "$folder")
repo=$(realpath "$(dirname "$0")/..")
if [[ $folder/ == "$repo"/* ]]; then
  echo "The folder is inside the repository; the diagram files stay outside it: $folder" >&2
  exit 2
fi
mkdir -p "$folder"

fetched=0 had=0 absent=0
for id in "${ids[@]}"; do
  if ! [[ $id =~ ^R-[A-Z]{3}-[0-9]+$ ]]; then
    echo "Not a stable id: $id" >&2
    exit 2
  fi
  file=$folder/$id.json
  if [[ -s $file ]]; then
    had=$((had + 1))
    continue
  fi
  (( fetched + absent == 0 )) || sleep "$pause"
  status=$(curl -sS -o "$file.part" -w '%{http_code}' "$base/$release/diagram/$id.json") || {
    rm -f "$file.part"
    echo "Fetching $id failed" >&2
    exit 1
  }
  case $status in
    200)
      if [[ $(head -c1 "$file.part") != "{" ]]; then
        rm -f "$file.part"
        echo "$id: the answer is not a diagram file" >&2
        exit 1
      fi
      mv "$file.part" "$file"
      fetched=$((fetched + 1)) ;;
    403|404)
      rm -f "$file.part"
      echo "no diagram: $id"
      absent=$((absent + 1)) ;;
    *)
      rm -f "$file.part"
      echo "$id: HTTP $status" >&2
      exit 1 ;;
  esac
done
echo "$fetched fetched, $had already there, $absent without a diagram"
