#!/usr/bin/env bash
# Fetches the diagram files a comparison or dry run needs (spec 001, T018; research R8)
# from the release's download directory, {base}/{release}/diagram/{stId}.json: one at a
# time, in sequence, with a pause between, into a folder outside this repository. A file
# already in the folder is not fetched again, so the folder records its release (RELEASE)
# and a folder of another release is refused. An id with no file (a reaction, or a pathway
# drawn only on a parent's diagram) is listed and skipped: a 404, or the download store's
# 403 for a missing file (its XML AccessDenied error). A file missed this way is not lost
# silently: layout.DiagramFiles refuses a pathway the graph says has a diagram but whose
# file is missing. Any other answer, or a failed or timed-out transfer, stops the run.
# Needs bash 4 and GNU coreutils (realpath -m).
# Usage: ci/fetch-diagrams.sh <release> <folder> <stId>...
#        ci/fetch-diagrams.sh <release> <folder> < ids.txt
set -euo pipefail
base=${DIAGRAM_BASE:-https://download.reactome.org}
pause=${DIAGRAM_PAUSE:-1}

usage() { sed -n '12,13s/^# //p' "$0" >&2; exit 2; }
[[ $# -ge 2 && $1 =~ ^[0-9]+$ ]] || usage
release=$1 folder=$2
shift 2
if (( $# )); then
  ids=("$@")
elif [[ -t 0 ]]; then
  usage
else
  mapfile -t ids < <(grep -oE 'R-[A-Z]{3}-[0-9]+' || true)
fi
(( ${#ids[@]} )) || { echo "No ids given" >&2; exit 2; }
for id in "${ids[@]}"; do
  [[ $id =~ ^R-[A-Z]{3}-[0-9]+$ ]] || { echo "Not a stable id: $id" >&2; exit 2; }
done

folder=$(realpath -m "$folder")
repo=$(realpath "$(dirname "$0")/..")
if [[ $folder/ == "$repo"/* ]]; then
  echo "The folder is inside the repository; the diagram files stay outside it: $folder" >&2
  exit 2
fi
mkdir -p "$folder"
if [[ -e $folder/RELEASE ]]; then
  had_release=$(<"$folder/RELEASE")
  if [[ $had_release != "$release" ]]; then
    echo "The folder holds release $had_release's diagram files, not release $release's: $folder" >&2
    exit 2
  fi
elif compgen -G "$folder/*.json" > /dev/null; then
  echo "The folder has diagram files of no recorded release: $folder" >&2
  exit 2
else
  echo "$release" > "$folder/RELEASE"
fi

part=
trap 'rm -f "$part"' EXIT
fetched=0 had=0 absent=0
for id in "${ids[@]}"; do
  file=$folder/$id.json
  if [[ -s $file ]]; then
    had=$((had + 1))
    continue
  fi
  (( fetched + absent == 0 )) || sleep "$pause"
  part=$file.part
  status=$(curl -sS --connect-timeout 30 --max-time 300 -o "$part" -w '%{http_code}' \
                "$base/$release/diagram/$id.json") || {
    echo "Fetching $id failed" >&2
    exit 1
  }
  if [[ $status == 200 ]]; then
    if [[ $(head -c1 "$part") != "{" ]]; then
      echo "$id: the answer does not start as a JSON object" >&2
      exit 1
    fi
    mv "$part" "$file"
    fetched=$((fetched + 1))
  elif [[ $status == 404 ]] || { [[ $status == 403 ]] && grep -q '<Code>AccessDenied</Code>' "$part"; }; then
    echo "no diagram: $id"
    absent=$((absent + 1))
  else
    echo "$id: HTTP $status" >&2
    exit 1
  fi
  rm -f "$part"
done
echo "$fetched fetched, $had already there, $absent without a diagram"
